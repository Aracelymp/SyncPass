package com.rojas.syncpass.data.repository

import androidx.room.withTransaction
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.local.AppDatabase
import com.rojas.syncpass.data.local.AttendanceDao
import com.rojas.syncpass.data.local.OperationType
import com.rojas.syncpass.data.local.PendingOperationDao
import com.rojas.syncpass.data.local.PendingOperationEntity
import com.rojas.syncpass.data.local.SurveyDao
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.local.SyncState
import com.rojas.syncpass.data.local.toEntity
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.SyncBatchRequest
import com.rojas.syncpass.data.remote.SyncOperationDto
import com.rojas.syncpass.data.remote.SyncPassApiService
import com.rojas.syncpass.data.session.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncOutcome {
    data class Success(val pushed: Int, val duplicates: Int, val downloaded: Int) : SyncOutcome
    data object Offline : SyncOutcome
    data object NoSession : SyncOutcome
    data object OfflineSession : SyncOutcome
    data class Error(val message: String) : SyncOutcome
}

class SyncRepository(
    private val api: SyncPassApiService,
    private val database: AppDatabase,
    private val pendingDao: PendingOperationDao,
    private val attendanceDao: AttendanceDao,
    private val surveyDao: SurveyDao,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) {
    private val mutex = Mutex()

    val pendingCount: Flow<Int> = pendingDao.observeCount()
    val pendingOperations: Flow<List<PendingOperationEntity>> = pendingDao.observeAll()
    val lastSync: Flow<Long?> = sessionManager.lastSync

    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        val session = sessionManager.currentSession() ?: return SyncOutcome.NoSession
        if (session.offline || session.token.isBlank()) return SyncOutcome.OfflineSession
        if (!networkMonitor.isCurrentlyOnline()) return SyncOutcome.Offline

        var pushed = 0
        var duplicates = 0
        try {
            val pending = pendingDao.first(100)
            if (pending.isNotEmpty()) {
                val operationsByClientId = pending.associateBy { it.clientId }
                val request = SyncBatchRequest(
                    pending.map { operation ->
                        SyncOperationDto(
                            clientId = operation.clientId,
                            type = operation.type,
                            eventId = operation.eventId,
                            attendanceClientId = operation.attendanceClientId,
                            attendanceServerId = operation.attendanceServerId,
                            qrCode = operation.qrCode,
                            participantName = operation.participantName,
                            participantEmail = operation.participantEmail,
                            participantDocument = operation.participantDocument,
                            rating = operation.rating,
                            organizationRating = operation.organizationRating,
                            impactRating = operation.impactRating,
                            comment = operation.comment,
                            wouldRecommend = operation.wouldRecommend,
                            occurredAt = operation.occurredAt
                        )
                    }
                )
                val response = api.syncBatch(request)
                for (result in response.results) {
                    val operation = operationsByClientId[result.clientId]
                    val surveyOperation = operation?.type == OperationType.SURVEY
                    when (result.status) {
                        "SYNCED", "DUPLICATE" -> {
                            if (surveyOperation) {
                                if (session.isCoordinator) {
                                    surveyDao.updateSyncState(
                                        result.clientId, SyncState.SYNCED.name, result.serverId, null
                                    )
                                } else {
                                    // Only after the server confirms the survey can the local
                                    // monitor copy be reduced to a receipt without content.
                                    surveyDao.markSyncedAndRedact(result.clientId, result.serverId)
                                }
                            } else {
                                attendanceDao.updateSyncState(
                                    result.clientId, SyncState.SYNCED.name, result.serverId, null
                                )
                            }
                            pendingDao.delete(result.clientId)
                            if (result.status == "SYNCED") pushed++ else duplicates++
                        }
                        else -> {
                            val message = result.message ?: "No se pudo sincronizar"
                            if (surveyOperation) {
                                surveyDao.updateSyncState(
                                    result.clientId, SyncState.ERROR.name, null, message
                                )
                            } else {
                                attendanceDao.updateSyncState(
                                    result.clientId, SyncState.ERROR.name, null, message
                                )
                            }
                            pendingDao.markError(result.clientId, message)
                        }
                    }
                }
            }

            var downloaded = 0
            val pull = api.syncPull()
            database.withTransaction {
                pull.projects.map { it.toEntity() }.takeIf { it.isNotEmpty() }?.let {
                    database.projectDao().upsertAll(it)
                }
                pull.events.map { it.toEntity() }.takeIf { it.isNotEmpty() }?.let {
                    database.eventDao().upsertAll(it)
                }
                pull.enrollments.map { it.toEntity() }.takeIf { it.isNotEmpty() }?.let {
                    database.enrollmentDao().upsertAll(it)
                }
                pull.attendances.forEach { dto ->
                    val existing = attendanceDao.findByClientId(dto.clientId)
                    attendanceDao.upsert(dto.toEntity(existing?.localId ?: 0L))
                    downloaded++
                }
                if (session.isCoordinator) {
                    pull.surveys.forEach { dto ->
                        val existing = surveyDao.findByClientId(dto.clientId)
                        surveyDao.upsert(dto.toEntity(existing?.localId ?: 0L))
                    }
                } else {
                    pull.surveyReceipts.orEmpty().forEach { receipt ->
                        val existing = receipt.attendanceClientId
                            ?.let { surveyDao.findByAttendanceClientId(it) }
                            ?: surveyDao.findByServerId(receipt.id)
                        if (existing != null) {
                            surveyDao.markSyncedAndRedact(existing.clientId, receipt.id)
                        } else if (receipt.attendanceClientId != null) {
                            surveyDao.upsert(
                                SurveyEntity(
                                    serverId = receipt.id,
                                    eventId = receipt.eventId,
                                    userId = null,
                                    participantName = "Participante",
                                    rating = 0,
                                    organizationRating = 0,
                                    impactRating = 0,
                                    comment = null,
                                    wouldRecommend = false,
                                    submittedAt = receipt.submittedAt,
                                    clientId = "receipt-${receipt.id}",
                                    syncState = SyncState.SYNCED.name,
                                    attendanceClientId = receipt.attendanceClientId,
                                    attendanceServerId = receipt.attendanceId,
                                    collectedByUserId = session.userId,
                                    redacted = true
                                )
                            )
                        }
                    }
                }
            }
            sessionManager.markLastSync()
            return SyncOutcome.Success(pushed, duplicates, downloaded)
        } catch (e: Exception) {
            return SyncOutcome.Error(ApiErrors.message(e))
        }
    }
}
