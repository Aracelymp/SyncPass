package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.local.AttendanceDao
import com.rojas.syncpass.data.local.EventDao
import com.rojas.syncpass.data.local.OperationType
import com.rojas.syncpass.data.local.PendingOperationDao
import com.rojas.syncpass.data.local.PendingOperationEntity
import com.rojas.syncpass.data.local.SurveyDao
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.session.Session
import java.util.UUID

sealed interface SurveyResult {
    data class Success(val survey: SurveyEntity) : SurveyResult
    data class Failure(val message: String) : SurveyResult
}

class SurveyRepository(
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val surveyDao: SurveyDao,
    private val pendingDao: PendingOperationDao
) {
    suspend fun submit(
        eventId: Long,
        session: Session,
        rating: Int,
        organizationRating: Int,
        impactRating: Int,
        comment: String?,
        wouldRecommend: Boolean
    ): SurveyResult {
        if (rating !in 1..5 || organizationRating !in 1..5 || impactRating !in 1..5) {
            return SurveyResult.Failure("Selecciona una calificación de 1 a 5 estrellas")
        }
        eventDao.findById(eventId)
            ?: return SurveyResult.Failure("El evento no está disponible en este dispositivo")
        surveyDao.findForUser(eventId, session.userId)?.let {
            return SurveyResult.Failure("Ya enviaste la encuesta de este evento")
        }

        val now = Dates.nowIso()
        val clientId = UUID.randomUUID().toString()
        val survey = SurveyEntity(
            eventId = eventId,
            userId = session.userId,
            participantName = session.fullName,
            rating = rating,
            organizationRating = organizationRating,
            impactRating = impactRating,
            comment = comment?.trim()?.takeIf { it.isNotBlank() },
            wouldRecommend = wouldRecommend,
            submittedAt = now,
            clientId = clientId,
            collectedByUserId = session.userId
        )
        surveyDao.upsert(survey)
        pendingDao.insert(
            PendingOperationEntity(
                clientId = clientId,
                type = OperationType.SURVEY,
                eventId = eventId,
                rating = rating,
                organizationRating = organizationRating,
                impactRating = impactRating,
                comment = survey.comment,
                wouldRecommend = wouldRecommend,
                occurredAt = now
            )
        )
        return SurveyResult.Success(survey)
    }

    suspend fun submitAssisted(
        eventId: Long,
        session: Session,
        attendanceClientId: String,
        rating: Int,
        organizationRating: Int,
        impactRating: Int,
        comment: String?,
        wouldRecommend: Boolean
    ): SurveyResult {
        if (rating !in 1..5 || organizationRating !in 1..5 || impactRating !in 1..5) {
            return SurveyResult.Failure("Selecciona una calificación de 1 a 5 estrellas")
        }
        eventDao.findById(eventId)
            ?: return SurveyResult.Failure("El evento no está disponible en este dispositivo")
        val attendance = attendanceDao.findByClientId(attendanceClientId)
            ?: return SurveyResult.Failure("La asistencia no está disponible en este dispositivo")
        if (attendance.eventId != eventId) {
            return SurveyResult.Failure("La asistencia no pertenece a este evento")
        }
        if (!attendance.canBeSurveyedBy(session)) {
            return SurveyResult.Failure("Solo puedes encuestar asistencias que tú registraste o escaneaste")
        }
        surveyDao.findByAttendanceClientId(attendance.clientId)?.let {
            return SurveyResult.Failure("Esta asistencia ya tiene una encuesta registrada")
        }
        attendance.userId?.let { userId ->
            surveyDao.findForUser(eventId, userId)?.let {
                return SurveyResult.Failure("El participante ya respondió la encuesta de este evento")
            }
        }

        val now = Dates.nowIso()
        val clientId = UUID.randomUUID().toString()
        val survey = SurveyEntity(
            eventId = eventId,
            userId = attendance.userId,
            participantName = attendance.participantName,
            rating = rating,
            organizationRating = organizationRating,
            impactRating = impactRating,
            comment = comment?.trim()?.takeIf { it.isNotBlank() },
            wouldRecommend = wouldRecommend,
            submittedAt = now,
            clientId = clientId,
            attendanceClientId = attendance.clientId,
            attendanceServerId = attendance.serverId,
            collectedByUserId = session.userId
        )
        surveyDao.upsert(survey)
        pendingDao.insert(
            PendingOperationEntity(
                clientId = clientId,
                type = OperationType.SURVEY,
                eventId = eventId,
                attendanceClientId = attendance.clientId,
                attendanceServerId = attendance.serverId,
                rating = rating,
                organizationRating = organizationRating,
                impactRating = impactRating,
                comment = survey.comment,
                wouldRecommend = wouldRecommend,
                occurredAt = now
            )
        )
        return SurveyResult.Success(survey)
    }
}
