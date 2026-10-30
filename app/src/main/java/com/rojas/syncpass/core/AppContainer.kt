package com.rojas.syncpass.core

import android.content.Context
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.core.auth.CognitoDirectAuthService
import com.rojas.syncpass.data.local.AppDatabase
import com.rojas.syncpass.data.local.DemoLocalSeeder
import com.rojas.syncpass.data.remote.NetworkModule
import com.rojas.syncpass.data.repository.AttendanceRepository
import com.rojas.syncpass.data.repository.AuthRepository
import com.rojas.syncpass.data.repository.DashboardRepository
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.ProjectRepository
import com.rojas.syncpass.data.repository.ReportRepository
import com.rojas.syncpass.data.repository.UserAdminRepository
import com.rojas.syncpass.data.repository.SessionRepository
import com.rojas.syncpass.data.repository.SurveyRepository
import com.rojas.syncpass.data.repository.SyncRepository
import com.rojas.syncpass.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container. Small enough not to need Hilt, explicit enough to test.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase = AppDatabase.get(appContext)
    val networkMonitor = NetworkMonitor(appContext)
    val sessionManager = SessionManager(appContext, applicationScope)
    val cognitoDirectAuthService = CognitoDirectAuthService()
    val network = NetworkModule { sessionManager.currentToken() }

val authRepository = AuthRepository(
    network.authApi,
    sessionManager,
    database.userDao(),
    database,
    networkMonitor,
    cognitoDirectAuthService
)
    val userAdminRepository = UserAdminRepository(network.api, network.authApi, networkMonitor)
    val projectRepository = ProjectRepository(network.api, database.projectDao())
    val eventRepository = EventRepository(network.api, database.eventDao(), database.enrollmentDao())
    val attendanceRepository = AttendanceRepository(
        network.api, database.eventDao(), database.attendanceDao(),
        database.pendingOperationDao(), networkMonitor
    )
    val surveyRepository = SurveyRepository(
        database.eventDao(), database.attendanceDao(), database.surveyDao(),
        database.pendingOperationDao()
    )
    val syncRepository = SyncRepository(
        network.api, database, database.pendingOperationDao(), database.attendanceDao(),
        database.surveyDao(), sessionManager, networkMonitor
    )
    val sessionRepository = SessionRepository(authRepository, database)
    val dashboardRepository = DashboardRepository(
        database.projectDao(), database.eventDao(), database.attendanceDao(),
        database.pendingOperationDao()
    )
    val reportRepository = ReportRepository(
        database.projectDao(), database.eventDao(), database.attendanceDao(), database.surveyDao()
    )

    init {
        applicationScope.launch { DemoLocalSeeder.seedIfEmpty(database) }
    }
}
