package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.local.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepositoryTest {

    private lateinit var repository: DashboardRepository

    private fun event(id: Long, startsAt: String) = EventEntity(
        id = id, projectId = 1L, projectName = "P", title = "E$id",
        description = "", location = "", startsAt = startsAt, endsAt = startsAt,
        capacity = 10, qrToken = "token-$id", status = "ACTIVO",
        enrolledCount = 0, attendedCount = 0, averageRating = 0.0
    )

    @Before
    fun setUp() {
        // Fechas deterministas para el test. El comportamiento real de Dates se valida en DatesTest.
        val now = LocalDateTime.of(2026, 11, 21, 12, 0)
        val upcomingStart = now.plusDays(1).toString()
        val todayStart = now.minusHours(2).toString()
        val events = listOf(event(1L, upcomingStart), event(2L, todayStart))

        mockkObject(Dates)
        every { Dates.isUpcoming(upcomingStart) } returns true
        every { Dates.isUpcoming(todayStart) } returns false
        every { Dates.isToday(todayStart) } returns true
        every { Dates.isToday(upcomingStart) } returns false

        val projectDao = mockk<ProjectDao>(relaxed = true)
        val eventDao = mockk<EventDao>(relaxed = true)
        val attendanceDao = mockk<AttendanceDao>(relaxed = true)
        val pendingDao = mockk<PendingOperationDao>(relaxed = true)

        every { projectDao.observeCount() } returns flowOf(3)
        every { projectDao.observeAll() } returns flowOf(emptyList<ProjectEntity>())
        every { eventDao.observeAll() } returns flowOf(events)
        every { attendanceDao.observeAll() } returns flowOf(emptyList<AttendanceEntity>())
        every { pendingDao.observeCount() } returns flowOf(5)

        repository = DashboardRepository(projectDao, eventDao, attendanceDao, pendingDao)
    }

    @After
    fun tearDown() {
        unmockkObject(Dates)
    }

    @Test
    fun `stats combines counts and filters upcoming and today events`() = runTest(UnconfinedTestDispatcher()) {
        val stats = repository.stats.first()

        assertEquals(3, stats.projectCount)
        assertEquals(2, stats.eventCount)
        assertEquals(0, stats.attendanceCount)
        assertEquals(5, stats.pendingCount)
        assertEquals(1, stats.upcomingEvents.size)
        assertEquals(1, stats.todayEvents.size)
    }

    @Test
    fun `projects exposes project dao flow`() = runTest(UnconfinedTestDispatcher()) {
        assertTrue(repository.projects.first().isEmpty())
    }
}
