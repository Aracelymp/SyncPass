package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.EnrollmentDao
import com.rojas.syncpass.data.local.EnrollmentEntity
import com.rojas.syncpass.data.local.EventDao
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.remote.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryTest {

    private lateinit var api: SyncPassApiService
    private lateinit var eventDao: EventDao
    private lateinit var enrollmentDao: EnrollmentDao
    private lateinit var repository: EventRepository

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        eventDao = mockk(relaxed = true)
        enrollmentDao = mockk(relaxed = true)
        every { eventDao.observeAll() } returns flowOf(emptyList<EventEntity>())
        repository = EventRepository(api, eventDao, enrollmentDao)
    }

    private fun eventDto(id: Long = 1L) = EventDto(
        id = id, projectId = 1L, projectName = "Proyecto", title = "Evento",
        description = "Desc", location = "Aula", startsAt = "2026-08-15T10:00",
        endsAt = "2026-08-15T12:00", capacity = 30, qrToken = "token-$id",
        status = "ACTIVO", enrolledCount = 0, attendedCount = 0, averageRating = null
    )

    private fun eventEntity(id: Long = 1L) = eventDto(id).let {
        EventEntity(
            id = it.id, projectId = it.projectId, projectName = it.projectName, title = it.title,
            description = it.description, location = it.location, startsAt = it.startsAt,
            endsAt = it.endsAt, capacity = it.capacity, qrToken = it.qrToken,
            status = it.status, enrolledCount = it.enrolledCount, attendedCount = it.attendedCount,
            averageRating = 0.0
        )
    }

    @Test
    fun `events exposes dao flow`() = runTest(UnconfinedTestDispatcher()) {
        assertTrue(repository.events.first().isEmpty())
    }

    @Test
    fun `eventsForProject filters by project`() = runTest(UnconfinedTestDispatcher()) {
        every { eventDao.observeByProject(1L) } returns flowOf(listOf(eventEntity()))
        assertEquals(1, repository.eventsForProject(1L).first().size)
    }

    @Test
    fun `refresh upserts events`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { api.events(null) } returns listOf(eventDto())
        coEvery { eventDao.upsertAll(any()) } just Runs

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        coVerify { eventDao.upsertAll(any()) }
    }

    @Test
    fun `refreshEvent stores event and returns detail`() = runTest(UnconfinedTestDispatcher()) {
        val detail = EventDetailDto(
            event = eventDto(),
            participants = emptyList(),
            surveySummary = null,
            enrolled = false,
            attended = false,
            surveyCompleted = false
        )
        coEvery { api.eventDetail(1L) } returns detail
        coEvery { eventDao.upsert(any()) } just Runs

        val result = repository.refreshEvent(1L)

        assertTrue(result.isSuccess)
        assertEquals(detail, result.getOrThrow())
    }

    @Test
    fun `create stores and returns event`() = runTest(UnconfinedTestDispatcher()) {
        val request = EventRequest(
            projectId = 1L, title = "Nuevo", description = "Desc", location = "Aula",
            startsAt = "2026-08-15T10:00", endsAt = "2026-08-15T12:00", capacity = 30,
            qrToken = "token", status = "ACTIVO"
        )
        coEvery { api.createEvent(request) } returns eventDto()
        coEvery { eventDao.upsert(any()) } just Runs

        val result = repository.create(request)

        assertTrue(result.isSuccess)
        assertEquals("Evento", result.getOrThrow().title)
    }

    @Test
    fun `create wraps error in RepositoryException`() = runTest(UnconfinedTestDispatcher()) {
        val request = EventRequest(
            projectId = 1L, title = "Nuevo", description = "Desc", location = "Aula",
            startsAt = "2026-08-15T10:00", endsAt = "2026-08-15T12:00", capacity = 30,
            qrToken = "token", status = "ACTIVO"
        )
        coEvery { api.createEvent(request) } throws RuntimeException("fail")

        val result = repository.create(request)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RepositoryException)
    }

    @Test
    fun `delete removes event locally`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { api.deleteEvent(1L) } returns MessageResponse("Eliminado")
        coEvery { eventDao.deleteById(1L) } just Runs

        val result = repository.delete(1L)

        assertTrue(result.isSuccess)
        assertEquals("Eliminado", result.getOrThrow())
    }

    @Test
    fun `enroll increments count when new enrollment`() = runTest(UnconfinedTestDispatcher()) {
        val event = eventEntity().copy(enrolledCount = 5)
        val slot = io.mockk.slot<EventEntity>()
        coEvery { enrollmentDao.findEnrolled(1L, 2L) } returns null
        coEvery { api.enroll(1L) } returns EnrollmentDto(
            id = 1L, eventId = 1L, userId = 2L, userName = "Monitor", status = "INSCRITO", createdAt = null
        )
        coEvery { eventDao.findById(1L) } returns event
        coEvery { eventDao.upsert(capture(slot)) } just Runs

        val result = repository.enroll(1L, 2L)

        assertTrue(result.isSuccess)
        assertEquals(6, slot.captured.enrolledCount)
    }

    @Test
    fun `enroll does not increment when already enrolled`() = runTest(UnconfinedTestDispatcher()) {
        val event = eventEntity().copy(enrolledCount = 5)
        coEvery { enrollmentDao.findEnrolled(1L, 2L) } returns EnrollmentEntity(
            id = 1L, eventId = 1L, userId = 2L, userName = "Monitor", status = "INSCRITO", createdAt = ""
        )
        coEvery { api.enroll(1L) } returns EnrollmentDto(
            id = 1L, eventId = 1L, userId = 2L, userName = "Monitor", status = "INSCRITO", createdAt = null
        )
        coEvery { eventDao.findById(1L) } returns event

        repository.enroll(1L, 2L)

        coVerify(exactly = 0) { eventDao.upsert(any<EventEntity>()) }
    }

    @Test
    fun `cancelEnrollment updates enrollment and decrements count`() = runTest(UnconfinedTestDispatcher()) {
        val event = eventEntity().copy(enrolledCount = 5)
        val enrollment = EnrollmentEntity(
            id = 1L, eventId = 1L, userId = 2L, userName = "Monitor", status = "INSCRITO", createdAt = ""
        )
        coEvery { api.cancelEnrollment(1L) } returns MessageResponse("Cancelado")
        coEvery { enrollmentDao.findEnrolled(1L, 2L) } returns enrollment
        coEvery { eventDao.findById(1L) } returns event

        val result = repository.cancelEnrollment(1L, 2L)

        assertTrue(result.isSuccess)
        coVerify { enrollmentDao.upsert(enrollment.copy(status = "CANCELADO")) }
        coVerify { eventDao.upsert(event.copy(enrolledCount = 4)) }
    }

    @Test
    fun `findEventByToken returns dao result`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { eventDao.findByQrToken("token") } returns eventEntity()

        val result = repository.findEventByToken("token")

        assertNotNull(result)
    }
}
