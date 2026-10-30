package com.rojas.syncpass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.data.session.SessionState
import com.rojas.syncpass.ui.components.BrandMark
import com.rojas.syncpass.ui.dashboard.DashboardScreen
import com.rojas.syncpass.ui.events.EventDetailScreen
import com.rojas.syncpass.ui.events.EventsScreen
import com.rojas.syncpass.ui.login.LoginScreen
import com.rojas.syncpass.ui.profile.ProfileScreen
import com.rojas.syncpass.ui.projects.ProjectDetailScreen
import com.rojas.syncpass.ui.projects.ProjectsScreen
import com.rojas.syncpass.ui.reports.ReportsScreen
import com.rojas.syncpass.ui.scanner.ScannerScreen
import com.rojas.syncpass.ui.survey.SurveyScreen
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.users.UsersScreen
import com.rojas.syncpass.ui.walkin.WalkInScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val EVENTS = "events"
    const val REPORTS = "reports"
    const val USERS = "users"
    const val PROFILE = "profile"

    const val PROJECT_DETAIL = "project/{projectId}"
    fun projectDetail(projectId: Long) = "project/$projectId"

    const val EVENT_DETAIL = "event/{eventId}"
    fun eventDetail(eventId: Long) = "event/$eventId"

    const val WALK_IN = "event/{eventId}/walk-in"
    fun walkIn(eventId: Long) = "event/$eventId/walk-in"

    const val SURVEY = "event/{eventId}/survey?attendanceClientId={attendanceClientId}"
    fun survey(eventId: Long, attendanceClientId: String? = null) =
        "event/$eventId/survey?attendanceClientId=${attendanceClientId.orEmpty()}"

    const val SCANNER = "scanner"
}

@Composable
fun SyncPassAppRoot(container: AppContainer) {
    val sessionState by container.sessionManager.state.collectAsStateWithLifecycle()
    when (val state = sessionState) {
        SessionState.Loading -> SplashScreen()
        SessionState.LoggedOut -> LoginScreen(container)
        is SessionState.LoggedIn -> MainShell(container, state.session)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandGradient),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(size = 82.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
            Text(
                "SyncPass",
                style = MaterialTheme.typography.displaySmall,
                color = OnBrand
            )
            Text(
                "Cargando tu sesión…",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBrand.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 18.dp))
            CircularProgressIndicator(color = OnBrand)
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun MainShell(container: AppContainer, session: Session) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val destinations = buildList {
        add(BottomDestination(Routes.DASHBOARD, "Inicio", Icons.Filled.Home))
        add(BottomDestination(Routes.PROJECTS, "Proyectos", Icons.Filled.Folder))
        add(BottomDestination(Routes.EVENTS, "Eventos", Icons.Filled.Event))
        if (session.isCoordinator) {
            add(BottomDestination(Routes.REPORTS, "Reportes", Icons.Filled.Insights))
            add(BottomDestination(Routes.USERS, "Usuarios", Icons.Filled.People))
        }
        add(BottomDestination(Routes.PROFILE, "Perfil", Icons.Filled.Person))
    }
    val topLevelRoutes = destinations.map { it.route }.toSet()
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigateTopLevel(navController, destination.route) },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    container = container,
                    session = session,
                    onEventClick = { navController.navigate(Routes.eventDetail(it)) },
                    onOpenProjects = { navigateTopLevel(navController, Routes.PROJECTS) },
                    onOpenEvents = { navigateTopLevel(navController, Routes.EVENTS) },
                    onOpenReports = { navigateTopLevel(navController, Routes.REPORTS) },
                    onScan = { navController.navigate(Routes.SCANNER) }
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    container = container,
                    session = session,
                    onProjectClick = { navController.navigate(Routes.projectDetail(it)) }
                )
            }
            composable(Routes.EVENTS) {
                EventsScreen(
                    container = container,
                    session = session,
                    onEventClick = { navController.navigate(Routes.eventDetail(it)) }
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen(
                    container = container,
                    session = session,
                    onEventClick = { navController.navigate(Routes.eventDetail(it)) }
                )
            }
            composable(Routes.USERS) {
                UsersScreen(container = container)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(container = container, session = session)
            }

            composable(
                route = Routes.PROJECT_DETAIL,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { entry ->
                val projectId = entry.arguments?.getLong("projectId") ?: 0L
                ProjectDetailScreen(
                    container = container,
                    session = session,
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                    onEventClick = { navController.navigate(Routes.eventDetail(it)) }
                )
            }

            composable(
                route = Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { entry ->
                val eventId = entry.arguments?.getLong("eventId") ?: 0L
                EventDetailScreen(
                    container = container,
                    session = session,
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onScan = { navController.navigate(Routes.SCANNER) },
                    onWalkIn = { navController.navigate(Routes.walkIn(it)) },
                    onSurvey = { id, attendanceClientId ->
                        navController.navigate(Routes.survey(id, attendanceClientId))
                    }
                )
            }

            composable(Routes.SCANNER) {
                ScannerScreen(
                    container = container,
                    session = session,
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { eventId -> navController.openEventDetail(eventId) }
                )
            }

            composable(
                route = Routes.WALK_IN,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { entry ->
                val eventId = entry.arguments?.getLong("eventId") ?: 0L
                WalkInScreen(
                    container = container,
                    session = session,
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { id -> navController.openEventDetail(id) }
                )
            }

            composable(
                route = Routes.SURVEY,
                arguments = listOf(
                    navArgument("eventId") { type = NavType.LongType },
                    navArgument("attendanceClientId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val eventId = entry.arguments?.getLong("eventId") ?: 0L
                val attendanceClientId = entry.arguments
                    ?.getString("attendanceClientId")
                    ?.takeIf { it.isNotBlank() }
                SurveyScreen(
                    container = container,
                    session = session,
                    eventId = eventId,
                    attendanceClientId = attendanceClientId,
                    onBack = { navController.popBackStack() },
                    onOpenEvent = { id -> navController.openEventDetail(id) }
                )
            }
        }
    }
}

private fun NavHostController.openEventDetail(eventId: Long) {
    // Scanner, walk-in and survey are transient screens. Remove the current one,
    // then replace the event detail underneath (if any) so the new detail is the
    // only event screen on the stack and one Back returns to the list/dashboard.
    popBackStack()
    if (currentBackStackEntry?.destination?.route == Routes.EVENT_DETAIL) {
        popBackStack()
    }
    navigate(Routes.eventDetail(eventId)) {
        launchSingleTop = true
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
