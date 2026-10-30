package com.rojas.syncpass.data.repository

import androidx.room.withTransaction
import com.rojas.syncpass.data.local.AppDatabase

class SessionRepository(
    private val authRepository: AuthRepository,
    private val database: AppDatabase
) {
    /** Clears user-specific cached data before ending the session. */
    suspend fun logout() {
        database.withTransaction {
            database.pendingOperationDao().clear()
            database.attendanceDao().clear()
            database.surveyDao().clear()
            database.enrollmentDao().clear()
        }
        authRepository.logout()
    }
}
