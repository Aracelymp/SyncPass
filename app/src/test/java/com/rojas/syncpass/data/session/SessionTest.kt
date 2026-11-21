package com.rojas.syncpass.data.session

import org.junit.Assert.*
import org.junit.Test

class SessionTest {

    private fun session(role: String) = Session(
        userId = 1L,
        username = "user",
        email = "",
        fullName = "",
        role = role,
        token = "token",
        offline = false
    )

    @Test
    fun `isCoordinator returns true only for coordinator role`() {
        assertTrue(session(Session.ROLE_COORDINATOR).isCoordinator)
        assertFalse(session(Session.ROLE_STUDENT).isCoordinator)
    }

    @Test
    fun `roleLabel returns coordinator label`() {
        assertTrue(session(Session.ROLE_COORDINATOR).roleLabel.contains("Docente"))
    }

    @Test
    fun `roleLabel returns student label`() {
        assertTrue(session(Session.ROLE_STUDENT).roleLabel.contains("Estudiante"))
    }
}
