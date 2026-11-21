package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.core.util.Qr
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.EventDto
import com.rojas.syncpass.data.remote.SyncPassApiService
import com.rojas.syncpass.data.session.Session
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceRepositoryTest {

    private lateinit var api: SyncPassApiService
    private lateinit var eventDao: EventDao
    private lateinit var attendanceDao: AttendanceDao
    private lateinit var pendingDao: PendingOperationDao
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var repository: AttendanceRepository

    private val session = Session(
        userId = 2L, username = "monitor", email = "m@test.com",
        fullName = "Monitor", role = Session.ROLE_STUDENT,
        token = "token", offline = false
    )

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        eventDao = mockk(relaxed = true)
        attendanceDao = mockk(relaxed = true)
        pendingDao = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        repository = AttendanceRepository(api, eventDao, attendanceDao, pendingDao, networkMonitor)
    }

    private fun event(id: Long = 1L) = EventEntity(
        id = id, projectId = 1L, projectName = "P", title = "E",
        description = "", location = "", startsAt = "2026-08-15T10:00", endsAt = "2026-08-15T12:00",
        capacity = 30, qrToken = "token-$id", status = "ACTIVO",
        enrolledCount = 0, attendedCount = 0, averageRating = 0.0
    )

    private fun eventDto(id: Long = 1L) = EventDto(
        id = id, projectId = 1L, projectName = "P", title = "E",
        description = "", location = "", startsAt = "2026-08-15T10:00", endsAt = "2026-08-15T12:00",
        capacity = 30, qrToken = "token-$id", status = "ACTIVO",
        enrolledCount = 0, attendedCount = 0, averageRating = null
    )

    @Test
    fun `recordQr returns failure for invalid qr`() = runTest {
        val result = repository.recordQr("invalid", session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordQr returns failure when event not found offline`() = runTest {
        val raw = Qr.payload(1L, "token-1")
        coEvery { eventDao.findById(1L) } returns null
        coEvery { eventDao.findByQrToken("token-1") } returns null
        every { networkMonitor.isCurrentlyOnline() } returns false

        val result = repository.recordQr(raw, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordQr fetches event when online`() = runTest {
        val raw = Qr.payload(1L, "token-1")
        coEvery { eventDao.findById(1L) } returns null
        coEvery { eventDao.findByQrToken("token-1") } returns null
        every { networkMonitor.isCurrentlyOnline() } returns true
        coEvery { api.event(1L) } returns eventDto()
        coEvery { eventDao.upsert(any()) } just Runs
        coEvery { attendanceDao.findForUser(1L, 2L) } returns null
        coEvery { attendanceDao.upsert(any()) } just Runs
        coEvery { eventDao.upsert(event().copy(attendedCount = 1)) } just Runs
        coEvery { pendingDao.insert(any()) } returns 1L

        val result = repository.recordQr(raw, session)

        assertTrue(result is AttendanceResult.Success)
    }

    @Test
    fun `recordQr returns failure when token mismatch`() = runTest {
        val raw = Qr.payload(1L, "wrong-token")
        coEvery { eventDao.findById(1L) } returns event()

        val result = repository.recordQr(raw, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordQr returns failure when event cancelled`() = runTest {
        val raw = Qr.payload(1L, "token-1")
        coEvery { eventDao.findById(1L) } returns event().copy(status = "CANCELADO")

        val result = repository.recordQr(raw, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordQr returns failure when already attended`() = runTest {
        val raw = Qr.payload(1L, "token-1")
        coEvery { eventDao.findById(1L) } returns event()
        coEvery { attendanceDao.findForUser(1L, 2L) } returns AttendanceEntity(
            eventId = 1L, eventTitle = "E", userId = 2L, participantName = "Monitor",
            participantEmail = null, participantDocument = null, method = "QR",
            registeredAt = "", registeredByName = null, clientId = "att-1"
        )

        val result = repository.recordQr(raw, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordWalkIn returns failure when event not found`() = runTest {
        coEvery { eventDao.findById(1L) } returns null

        val result = repository.recordWalkIn(1L, "Ana", null, null, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordWalkIn returns failure when name blank`() = runTest {
        coEvery { eventDao.findById(1L) } returns event()

        val result = repository.recordWalkIn(1L, "   ", null, null, session)

        assertTrue(result is AttendanceResult.Failure)
    }

    @Test
    fun `recordWalkIn success creates attendance and pending operation`() = runTest {
        coEvery { eventDao.findById(1L) } returns event()
        coEvery { attendanceDao.upsert(any()) } just Runs
        coEvery { eventDao.upsert(any()) } just Runs
        coEvery { pendingDao.insert(any()) } returns 1L
        every { networkMonitor.isCurrentlyOnline() } returns true

        val result = repository.recordWalkIn(1L, "Ana", "ana@test.com", "123", session)

        assertTrue(result is AttendanceResult.Success)
        coVerify { attendanceDao.upsert(any()) }
        coVerify { pendingDao.insert(any()) }
    }
}
