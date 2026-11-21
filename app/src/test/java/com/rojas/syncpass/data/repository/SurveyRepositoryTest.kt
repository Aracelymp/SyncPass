package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.session.Session
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SurveyRepositoryTest {

    private lateinit var eventDao: EventDao
    private lateinit var attendanceDao: AttendanceDao
    private lateinit var surveyDao: SurveyDao
    private lateinit var pendingDao: PendingOperationDao
    private lateinit var repository: SurveyRepository

    private val session = Session(
        userId = 2L, username = "monitor", email = "m@test.com",
        fullName = "Monitor", role = Session.ROLE_STUDENT,
        token = "token", offline = false
    )

    @Before
    fun setUp() {
        eventDao = mockk(relaxed = true)
        attendanceDao = mockk(relaxed = true)
        surveyDao = mockk(relaxed = true)
        pendingDao = mockk(relaxed = true)
        repository = SurveyRepository(eventDao, attendanceDao, surveyDao, pendingDao)
    }

    @Test
    fun `submit returns failure when rating invalid`() = runTest {
        val result = repository.submit(1L, session, 0, 3, 3, null, true)

        assertTrue(result is SurveyResult.Failure)
    }

    @Test
    fun `submit returns failure when event not found`() = runTest {
        coEvery { eventDao.findById(1L) } returns null

        val result = repository.submit(1L, session, 4, 4, 4, null, true)

        assertTrue(result is SurveyResult.Failure)
    }

    @Test
    fun `submit returns failure when already surveyed`() = runTest {
        coEvery { eventDao.findById(1L) } returns EventEntity(
            id = 1L, projectId = 1L, projectName = "P", title = "E",
            description = "", location = "", startsAt = "", endsAt = "",
            capacity = 10, qrToken = "", status = "ACTIVO",
            enrolledCount = 0, attendedCount = 0, averageRating = 0.0
        )
        coEvery { surveyDao.findForUser(1L, 2L) } returns SurveyEntity(
            eventId = 1L, userId = 2L, participantName = "", rating = 5,
            organizationRating = 5, impactRating = 5, comment = null,
            wouldRecommend = true, submittedAt = "", clientId = "s1"
        )

        val result = repository.submit(1L, session, 4, 4, 4, null, true)

        assertTrue(result is SurveyResult.Failure)
    }

    @Test
    fun `submit success stores survey and pending operation`() = runTest {
        coEvery { eventDao.findById(1L) } returns EventEntity(
            id = 1L, projectId = 1L, projectName = "P", title = "E",
            description = "", location = "", startsAt = "", endsAt = "",
            capacity = 10, qrToken = "", status = "ACTIVO",
            enrolledCount = 0, attendedCount = 0, averageRating = 0.0
        )
        coEvery { surveyDao.findForUser(1L, 2L) } returns null
        coEvery { surveyDao.upsert(any()) } just Runs
        coEvery { pendingDao.insert(any()) } returns 1L

        val result = repository.submit(1L, session, 4, 4, 4, "Bien", true)

        assertTrue(result is SurveyResult.Success)
        coVerify { surveyDao.upsert(any()) }
        coVerify { pendingDao.insert(any()) }
    }

    @Test
    fun `submitAssisted returns failure when attendance not found`() = runTest {
        coEvery { eventDao.findById(1L) } returns EventEntity(
            id = 1L, projectId = 1L, projectName = "P", title = "E",
            description = "", location = "", startsAt = "", endsAt = "",
            capacity = 10, qrToken = "", status = "ACTIVO",
            enrolledCount = 0, attendedCount = 0, averageRating = 0.0
        )
        coEvery { attendanceDao.findByClientId("att-1") } returns null

        val result = repository.submitAssisted(1L, session, "att-1", 4, 4, 4, null, true)

        assertTrue(result is SurveyResult.Failure)
    }

    @Test
    fun `submitAssisted returns failure when attendance belongs to different event`() = runTest {
        coEvery { eventDao.findById(1L) } returns EventEntity(
            id = 1L, projectId = 1L, projectName = "P", title = "E",
            description = "", location = "", startsAt = "", endsAt = "",
            capacity = 10, qrToken = "", status = "ACTIVO",
            enrolledCount = 0, attendedCount = 0, averageRating = 0.0
        )
        coEvery { attendanceDao.findByClientId("att-1") } returns AttendanceEntity(
            eventId = 2L, eventTitle = "Otro", userId = null, participantName = "Ana",
            participantEmail = null, participantDocument = null, method = "QR",
            registeredAt = "", registeredByName = null, clientId = "att-1",
            registeredByUserId = 2L
        )

        val result = repository.submitAssisted(1L, session, "att-1", 4, 4, 4, null, true)

        assertTrue(result is SurveyResult.Failure)
    }

    @Test
    fun `submitAssisted success for monitor own attendance`() = runTest {
        coEvery { eventDao.findById(1L) } returns EventEntity(
            id = 1L, projectId = 1L, projectName = "P", title = "E",
            description = "", location = "", startsAt = "", endsAt = "",
            capacity = 10, qrToken = "", status = "ACTIVO",
            enrolledCount = 0, attendedCount = 0, averageRating = 0.0
        )
        coEvery { attendanceDao.findByClientId("att-1") } returns AttendanceEntity(
            eventId = 1L, eventTitle = "E", userId = null, participantName = "Ana",
            participantEmail = null, participantDocument = null, method = "QR",
            registeredAt = "", registeredByName = null, clientId = "att-1",
            registeredByUserId = 2L
        )
        coEvery { surveyDao.findByAttendanceClientId("att-1") } returns null
        coEvery { surveyDao.upsert(any()) } just Runs
        coEvery { pendingDao.insert(any()) } returns 1L

        val result = repository.submitAssisted(1L, session, "att-1", 4, 4, 4, null, true)

        assertTrue(result is SurveyResult.Success)
        coVerify { surveyDao.upsert(any()) }
        coVerify { pendingDao.insert(any()) }
    }
}
