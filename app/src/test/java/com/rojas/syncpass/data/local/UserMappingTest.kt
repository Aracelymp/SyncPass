package com.rojas.syncpass.data.local

import com.rojas.syncpass.core.util.Hash
import com.rojas.syncpass.data.remote.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMappingTest {

    private val storedHash = Hash.localPassword("estudiante", "Demo1234")

    @Test
    fun `cognito update preserves the offline hash of the same user`() {
        val local = localUser(id = 3L, hash = storedHash)

        val updated = remoteUser(id = 3L).toEntity(loginAt = 200L, existing = local)

        assertEquals(storedHash, updated.localPasswordHash)
        assertEquals(200L, updated.lastLoginAt)
    }

    @Test
    fun `cognito update does not reuse a hash from another user`() {
        val otherLocalUser = localUser(id = 99L, hash = storedHash)

        val updated = remoteUser(id = 3L).toEntity(loginAt = 200L, existing = otherLocalUser)

        assertNull(updated.localPasswordHash)
    }

    @Test
    fun `password login recalculates the offline hash`() {
        val local = localUser(id = 3L, hash = "previous-hash")

        val updated = remoteUser(id = 3L).toEntity(
            password = "Demo1234",
            loginAt = 200L,
            existing = local
        )

        assertEquals(storedHash, updated.localPasswordHash)
        assertNotEquals("previous-hash", updated.localPasswordHash)
    }

    @Test
    fun `new cognito user has no offline hash`() {
        val created = remoteUser(id = 3L).toEntity(loginAt = 200L)

        assertNull(created.localPasswordHash)
    }

    private fun remoteUser(id: Long) = UserDto(
        id = id,
        username = "estudiante",
        email = "estudiante@syncpass.demo",
        fullName = "Ana Torres",
        role = "ESTUDIANTE_VOLUNTARIO_MONITOR"
    )

    private fun localUser(id: Long, hash: String) = UserEntity(
        id = id,
        username = "estudiante",
        email = "estudiante@syncpass.demo",
        fullName = "Ana Torres",
        role = "ESTUDIANTE_VOLUNTARIO_MONITOR",
        localPasswordHash = hash,
        lastLoginAt = 100L
    )
}
