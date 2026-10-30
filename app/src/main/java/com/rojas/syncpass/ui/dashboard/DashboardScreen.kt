package com.rojas.syncpass.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.Avatar
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.EventCard
import com.rojas.syncpass.ui.components.GradientHeader
import com.rojas.syncpass.ui.components.QuickAction
import com.rojas.syncpass.ui.components.SectionTitle
import com.rojas.syncpass.ui.components.StatTile
import com.rojas.syncpass.ui.components.StatusPill
import com.rojas.syncpass.ui.components.SyncPill
import com.rojas.syncpass.ui.components.ThinProgress
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    container: AppContainer,
    session: Session,
    onEventClick: (Long) -> Unit,
    onOpenProjects: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenReports: () -> Unit,
    onScan: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.dashboardRepository,
                    container.projectRepository,
                    container.eventRepository,
                    container.syncRepository,
                    container.networkMonitor
                )
            }
        }
    )
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            GradientHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Avatar(session.fullName, size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SyncPass",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnBrand.copy(alpha = 0.72f)
                        )
                        Text(
                            "Hola, ${session.fullName.substringBefore(' ')}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnBrand
                        )
                        Text(
                            session.roleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }, enabled = !refreshing) {
                        if (refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = OnBrand
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = OnBrand)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SyncPill(online = online, pending = pending)
                    Text(
                        text = Dates.relativeLastSync(lastSync),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBrand.copy(alpha = 0.72f)
                    )
                }
                if (session.offline) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.EventAvailable,
                            contentDescription = null,
                            tint = OnBrand,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Sesión sin conexión: puedes registrar asistencia. Vuelve a iniciar sesión con internet para sincronizar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.86f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Proyectos activos",
                        value = stats?.projectCount?.toString() ?: "—",
                        icon = Icons.Filled.Folder,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Eventos registrados",
                        value = stats?.eventCount?.toString() ?: "—",
                        icon = Icons.Filled.Event,
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = "Asistencias",
                        value = stats?.attendanceCount?.toString() ?: "—",
                        icon = Icons.Filled.Groups,
                        modifier = Modifier.weight(1f),
                        accent = SuccessGreen
                    )
                    StatTile(
                        label = "Pendientes por sincronizar",
                        value = stats?.pendingCount?.toString() ?: "—",
                        icon = Icons.Filled.AddCircle,
                        modifier = Modifier.weight(1f),
                        accent = WarningAmber
                    )
                }

                SectionTitle("Acciones rápidas")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        title = "Escanear QR",
                        subtitle = "Registra tu asistencia al instante",
                        icon = Icons.Filled.QrCodeScanner,
                        onClick = onScan,
                        modifier = Modifier.weight(1f),
                        highlighted = true
                    )
                    QuickAction(
                        title = "Proyectos",
                        subtitle = "Explora el impacto comunitario",
                        icon = Icons.Filled.Folder,
                        onClick = onOpenProjects,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (session.isCoordinator) {
                    QuickAction(
                        title = "Reportes de impacto",
                        subtitle = "Asistencia, satisfacción y recomendación por evento",
                        icon = Icons.Filled.Assessment,
                        onClick = onOpenReports,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (stats != null) {
                    SectionTitle("Hoy")
                    if (stats!!.todayEvents.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "No hay eventos programados para hoy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        stats!!.todayEvents.forEach { event ->
                            EventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                showRating = session.isCoordinator
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Próximos eventos", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Ver todos",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = onOpenEvents)
                                .padding(8.dp)
                        )
                    }
                    if (stats!!.upcomingEvents.isEmpty()) {
                        EmptyState(
                            title = "Sin próximos eventos",
                            message = "Cuando se programen nuevos eventos aparecerán aquí."
                        )
                    } else {
                        stats!!.upcomingEvents.forEach { event ->
                            EventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                showRating = session.isCoordinator
                            )
                        }
                    }

                    val attended = stats!!.attendanceCount
                    val events = stats!!.eventCount
                    if (events > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Participación general",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "$attended asistencias",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            ThinProgress(
                                value = (attended.toFloat() / (events * 10f)).coerceIn(0f, 1f)
                            )
                            Text(
                                "Los datos se calculan con la información guardada en el dispositivo y se actualizan al sincronizar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}
