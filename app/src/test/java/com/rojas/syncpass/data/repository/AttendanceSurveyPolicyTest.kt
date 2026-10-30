package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.session.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSurveyPolicyTest {

    private val coordinator = session(1L, Session.ROLE_COORDINATOR)
    private val monitor = session(2L, Session.ROLE_STUDENT)
    private val otherMonitor = session(3L, Session.ROLE_STUDENT)

    @Test
    fun `coordinator can survey any attendance`() {
        assertTrue(attendance(registeredBy = otherMonitor.userId).canBeSurveyedBy(coordinator))
        assertTrue(attendance(registeredBy = null, participantUser = null).canBeSurveyedBy(coordinator))
    }

    @Test
    fun `monitor can survey a walk-in they registered`() {
        assertTrue(attendance(registeredBy = monitor.userId).canBeSurveyedBy(monitor))
    }

    @Test
    fun `monitor can survey their own legacy attendance without registeredByUserId`() {
        assertTrue(
            attendance(registeredBy = null, participantUser = monitor.userId)
                .canBeSurveyedBy(monitor)
        )
    }

    @Test
    fun `monitor cannot survey another monitor attendance`() {
        assertFalse(attendance(registeredBy = otherMonitor.userId).canBeSurveyedBy(monitor))
        assertFalse(
            attendance(registeredBy = null, participantUser = otherMonitor.userId)
                .canBeSurveyedBy(monitor)
        )
    }

    @Test
    fun `monitor cannot survey an anonymous attendance registered by someone else`() {
        assertFalse(attendance(registeredBy = null, participantUser = null).canBeSurveyedBy(monitor))
        assertFalse(attendance(registeredBy = coordinator.userId).canBeSurveyedBy(monitor))
    }

    private fun attendance(registeredBy: Long?, participantUser: Long? = null) = AttendanceEntity(
        eventId = 10L,
        eventTitle = "Evento",
        userId = participantUser,
        participantName = "Participante",
        participantEmail = null,
        participantDocument = null,
        method = "NO_INSCRITO",
        registeredAt = "2026-10-23T10:00:00",
        registeredByName = "Monitor",
        clientId = "attendance-1",
        registeredByUserId = registeredBy
    )

    private fun session(userId: Long, role: String) = Session(
        userId = userId,
        username = "user$userId",
        email = "user$userId@syncpass.demo",
        fullName = "Usuario $userId",
        role = role,
        token = "token",
        offline = false
    )
}
