package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var eventDao: EventDao
    private lateinit var attendanceDao: AttendanceDao
    private lateinit var surveyDao: SurveyDao
    private lateinit var repository: ReportRepository

    @Before
    fun setUp() {
        projectDao = mockk(relaxed = true)
        eventDao = mockk(relaxed = true)
        attendanceDao = mockk(relaxed = true)
        surveyDao = mockk(relaxed = true)
        repository = ReportRepository(projectDao, eventDao, attendanceDao, surveyDao)
    }

    private fun event(id: Long, startsAt: String, enrolled: Int = 0) = EventEntity(
        id = id, projectId = 1L, projectName = "P", title = "E$id",
        description = "", location = "", startsAt = startsAt, endsAt = startsAt,
        capacity = 10, qrToken = "token-$id", status = "ACTIVO",
        enrolledCount = enrolled, attendedCount = 0, averageRating = 0.0
    )

    private fun attendance(eventId: Long, userId: Long? = null, email: String? = null) = AttendanceEntity(
        eventId = eventId, eventTitle = "E$eventId", userId = userId,
        participantName = "Ana", participantEmail = email, participantDocument = null,
        method = "QR", registeredAt = "2026-08-15T10:00", registeredByName = null,
        clientId = "att-$eventId", registeredByUserId = null
    )

    private fun survey(eventId: Long, rating: Int = 5, recommend: Boolean = true) = SurveyEntity(
        eventId = eventId, userId = null, participantName = "Ana",
        rating = rating, organizationRating = rating, impactRating = rating,
        comment = null, wouldRecommend = recommend, submittedAt = "2026-08-15T11:00",
        clientId = "srv-$eventId"
    )

    @Test
    fun `observe ALL includes all events`() = runTest(UnconfinedTestDispatcher()) {
        val events = listOf(event(1L, "2026-08-15T10:00", 2), event(2L, "2026-07-01T10:00", 1))
        every { projectDao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(listOf(attendance(1L)))
        every { surveyDao.observeAll() } returns flowOf(listOf(survey(1L)))

        val metrics = repository.observe(ReportPeriod.ALL).first()

        assertEquals(2, metrics.totalEvents)
        assertEquals(1, metrics.totalAttendances)
        assertEquals(2, metrics.events.size)
    }

    @Test
    fun `observe THIS_MONTH filters current month`() = runTest(UnconfinedTestDispatcher()) {
        val now = java.time.LocalDateTime.now()
        val events = listOf(
            event(1L, now.toString()),
            event(2L, now.minusMonths(1).toString())
        )
        every { projectDao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(emptyList<AttendanceEntity>())
        every { surveyDao.observeAll() } returns flowOf(emptyList<SurveyEntity>())

        val metrics = repository.observe(ReportPeriod.THIS_MONTH).first()

        assertEquals(1, metrics.totalEvents)
    }

    @Test
    fun `observe LAST_30_DAYS filters recent events`() = runTest(UnconfinedTestDispatcher()) {
        val now = java.time.LocalDateTime.now()
        val events = listOf(
            event(1L, now.minusDays(5).toString()),
            event(2L, now.minusDays(40).toString())
        )
        every { projectDao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(emptyList<AttendanceEntity>())
        every { surveyDao.observeAll() } returns flowOf(emptyList<SurveyEntity>())

        val metrics = repository.observe(ReportPeriod.LAST_30_DAYS).first()

        assertEquals(1, metrics.totalEvents)
    }

    @Test
    fun `attendance rate is capped at 100`() = runTest(UnconfinedTestDispatcher()) {
        val events = listOf(event(1L, "2026-08-15T10:00", enrolled = 1))
        every { projectDao.observeAll() } returns flowOf(emptyList())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(listOf(attendance(1L), attendance(1L)))
        every { surveyDao.observeAll() } returns flowOf(emptyList())

        val metrics = repository.observe(ReportPeriod.ALL).first()

        assertEquals(100.0, metrics.attendanceRate, 0.01)
    }

    @Test
    fun `empty surveys yield zero satisfaction and recommend rate`() = runTest(UnconfinedTestDispatcher()) {
        val events = listOf(event(1L, "2026-08-15T10:00", 0))
        every { projectDao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(emptyList<AttendanceEntity>())
        every { surveyDao.observeAll() } returns flowOf(emptyList<SurveyEntity>())

        val metrics = repository.observe(ReportPeriod.ALL).first()

        assertEquals(0.0, metrics.averageSatisfaction, 0.01)
        assertEquals(0.0, metrics.recommendRate, 0.01)
        assertTrue(metrics.events.isNotEmpty())
    }
}
