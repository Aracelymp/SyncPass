package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.EnrollmentDao
import com.rojas.syncpass.data.local.EnrollmentEntity
import com.rojas.syncpass.data.local.EventDao
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.toEntity
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.EventDetailDto
import com.rojas.syncpass.data.remote.EventDto
import com.rojas.syncpass.data.remote.EventRequest
import com.rojas.syncpass.data.remote.SyncPassApiService
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val api: SyncPassApiService,
    private val eventDao: EventDao,
    private val enrollmentDao: EnrollmentDao
) {
    val events: Flow<List<EventEntity>> = eventDao.observeAll()

    fun event(id: Long): Flow<EventEntity?> = eventDao.observeById(id)

    fun eventsForProject(projectId: Long): Flow<List<EventEntity>> =
        eventDao.observeByProject(projectId)

    fun enrollmentsForEvent(eventId: Long): Flow<List<EnrollmentEntity>> =
        enrollmentDao.observeByEvent(eventId)

    fun myEnrollments(userId: Long): Flow<List<EnrollmentEntity>> =
        enrollmentDao.observeByUser(userId)

    fun isEnrolled(eventId: Long, userId: Long): Flow<EnrollmentEntity?> =
        enrollmentDao.observeEnrolled(eventId, userId)

    suspend fun refresh(): Result<Unit> = runCatching {
        eventDao.upsertAll(api.events().map(EventDto::toEntity))
    }

    suspend fun refreshEvent(id: Long): Result<EventDetailDto> = runCatching {
        val detail = api.eventDetail(id)
        eventDao.upsert(detail.event.toEntity())
        detail
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun create(request: EventRequest): Result<EventEntity> = runCatching {
        val entity = api.createEvent(request).toEntity()
        eventDao.upsert(entity)
        entity
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun update(id: Long, request: EventRequest): Result<EventEntity> = runCatching {
        val entity = api.updateEvent(id, request).toEntity()
        eventDao.upsert(entity)
        entity
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun delete(id: Long): Result<String> = runCatching {
        val response = api.deleteEvent(id)
        eventDao.deleteById(id)
        response.message
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun enroll(eventId: Long, userId: Long): Result<Unit> = runCatching {
        val alreadyEnrolled = enrollmentDao.findEnrolled(eventId, userId) != null
        val enrollment = api.enroll(eventId).toEntity()
        enrollmentDao.upsert(enrollment)
        eventDao.findById(eventId)?.let { event ->
            if (enrollment.status == "INSCRITO" && !alreadyEnrolled) {
                eventDao.upsert(event.copy(enrolledCount = event.enrolledCount + 1))
            }
        }
        Unit
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun cancelEnrollment(eventId: Long, userId: Long): Result<Unit> = runCatching {
        api.cancelEnrollment(eventId)
        enrollmentDao.findEnrolled(eventId, userId)?.let { enrollment ->
            enrollmentDao.upsert(enrollment.copy(status = "CANCELADO"))
            eventDao.findById(eventId)?.let { event ->
                eventDao.upsert(event.copy(enrolledCount = (event.enrolledCount - 1).coerceAtLeast(0)))
            }
        }
        Unit
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun cachedEvent(id: Long): EventEntity? = eventDao.findById(id)

    suspend fun findEventByToken(token: String): EventEntity? = eventDao.findByQrToken(token)
}
