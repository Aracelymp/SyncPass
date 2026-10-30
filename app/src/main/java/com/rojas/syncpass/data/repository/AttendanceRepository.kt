package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.core.util.Qr
import com.rojas.syncpass.data.local.AttendanceDao
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EventDao
import com.rojas.syncpass.data.local.OperationType
import com.rojas.syncpass.data.local.PendingOperationDao
import com.rojas.syncpass.data.local.PendingOperationEntity
import com.rojas.syncpass.data.local.toEntity
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.SyncPassApiService
import com.rojas.syncpass.data.session.Session
import java.util.UUID

sealed interface AttendanceResult {
    data class Success(val attendance: AttendanceEntity, val eventId: Long, val online: Boolean) : AttendanceResult
    data class Failure(val message: String) : AttendanceResult
}

class AttendanceRepository(
    private val api: SyncPassApiService,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val pendingDao: PendingOperationDao,
    private val networkMonitor: NetworkMonitor
) {

    suspend fun recordQr(rawCode: String, session: Session): AttendanceResult {
        val payload = Qr.parse(rawCode)
            ?: return AttendanceResult.Failure("El código escaneado no es válido")
        val event = if (payload.eventId != null) {
            eventDao.findById(payload.eventId)
        } else {
            eventDao.findByQrToken(payload.token)
        } ?: fetchEvent(payload.eventId)
            ?: return AttendanceResult.Failure(
                "El evento no está disponible sin conexión. Sincroniza los eventos e intenta de nuevo."
            )

        if (event.qrToken != payload.token) {
            return AttendanceResult.Failure("El código QR no corresponde a este evento")
        }
        if (event.status == "CANCELADO") {
            return AttendanceResult.Failure("Este evento fue cancelado")
        }
        attendanceDao.findForUser(event.id, session.userId)?.let {
            return AttendanceResult.Failure("Ya registraste tu asistencia a este evento")
        }

        val now = Dates.nowIso()
        val clientId = UUID.randomUUID().toString()
        val attendance = AttendanceEntity(
            eventId = event.id,
            eventTitle = event.title,
            userId = session.userId,
            participantName = session.fullName,
            participantEmail = session.email,
            participantDocument = null,
            method = "QR",
            registeredAt = now,
            registeredByName = session.fullName,
            clientId = clientId,
            registeredByUserId = session.userId
        )
        attendanceDao.upsert(attendance)
        eventDao.upsert(event.copy(attendedCount = event.attendedCount + 1))
        pendingDao.insert(
            PendingOperationEntity(
                clientId = clientId,
                type = OperationType.ATTENDANCE_QR,
                eventId = event.id,
                qrCode = Qr.payload(event.id, event.qrToken),
                occurredAt = now
            )
        )
        return AttendanceResult.Success(attendance, event.id, networkMonitor.isCurrentlyOnline())
    }

    suspend fun recordWalkIn(
        eventId: Long,
        name: String,
        email: String?,
        document: String?,
        session: Session
    ): AttendanceResult {
        val event = eventDao.findById(eventId)
            ?: return AttendanceResult.Failure("El evento no está disponible en este dispositivo")
        if (name.isBlank()) {
            return AttendanceResult.Failure("Ingresa el nombre del participante")
        }
        val now = Dates.nowIso()
        val clientId = UUID.randomUUID().toString()
        val attendance = AttendanceEntity(
            eventId = event.id,
            eventTitle = event.title,
            userId = null,
            participantName = name.trim(),
            participantEmail = email?.trim()?.takeIf { it.isNotBlank() },
            participantDocument = document?.trim()?.takeIf { it.isNotBlank() },
            method = "NO_INSCRITO",
            registeredAt = now,
            registeredByName = session.fullName,
            clientId = clientId,
            registeredByUserId = session.userId
        )
        attendanceDao.upsert(attendance)
        eventDao.upsert(event.copy(attendedCount = event.attendedCount + 1))
        pendingDao.insert(
            PendingOperationEntity(
                clientId = clientId,
                type = OperationType.ATTENDANCE_WALK_IN,
                eventId = event.id,
                participantName = attendance.participantName,
                participantEmail = attendance.participantEmail,
                participantDocument = attendance.participantDocument,
                occurredAt = now
            )
        )
        return AttendanceResult.Success(attendance, event.id, networkMonitor.isCurrentlyOnline())
    }

    private suspend fun fetchEvent(eventId: Long?): com.rojas.syncpass.data.local.EventEntity? {
        if (eventId == null || !networkMonitor.isCurrentlyOnline()) return null
        return try {
            api.event(eventId).toEntity().also { eventDao.upsert(it) }
        } catch (_: Exception) {
            null
        }
    }
}
