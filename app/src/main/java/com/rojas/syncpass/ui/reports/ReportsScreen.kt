package com.rojas.syncpass.ui.reports

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.repository.ReportPeriod
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.GradientHeader
import com.rojas.syncpass.ui.components.RatingStars
import com.rojas.syncpass.ui.components.ThinProgress
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun ReportsScreen(
    container: AppContainer,
    session: Session,
    onEventClick: (Long) -> Unit
) {
    val viewModel: ReportsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReportsViewModel(container.reportRepository) }
        }
    )
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!session.isCoordinator) {
        Column(Modifier.fillMaxSize()) {
            GradientHeader {
                Text("Reportes", style = MaterialTheme.typography.headlineMedium, color = OnBrand)
            }
            EmptyState(
                title = "Acceso restringido",
                message = "Los reportes de impacto están disponibles solo para docentes coordinadores.",
                icon = Icons.Filled.Assessment
            )
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            GradientHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Reportes de impacto",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OnBrand
                        )
                        Text(
                            "Asistencia y satisfacción calculadas en el dispositivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.8f)
                        )
                    }
                    Icon(Icons.Filled.Assessment, null, tint = OnBrand, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    HeaderMetric("${metrics.totalAttendances}", "Asistencias")
                    HeaderMetric("${metrics.totalEvents}", "Eventos")
                    HeaderMetric(
                        "${String.format(Locale.US, "%.0f", metrics.averageSatisfaction * 20)}%",
                        "Satisfacción"
                    )
                }
            }

            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = period == ReportPeriod.ALL,
                        onClick = { viewModel.setPeriod(ReportPeriod.ALL) },
                        label = { Text("Todo") }
                    )
                    FilterChip(
                        selected = period == ReportPeriod.THIS_MONTH,
                        onClick = { viewModel.setPeriod(ReportPeriod.THIS_MONTH) },
                        label = { Text("Este mes") }
                    )
                    FilterChip(
                        selected = period == ReportPeriod.LAST_30_DAYS,
                        onClick = { viewModel.setPeriod(ReportPeriod.LAST_30_DAYS) },
                        label = { Text("Últimos 30 días") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        "Proyectos",
                        "${metrics.totalProjects}",
                        Icons.Filled.Folder,
                        Modifier.weight(1f)
                    )
                    MetricCard(
                        "Inscripciones",
                        "${metrics.totalEnrollments}",
                        Icons.Filled.Groups,
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        "Participantes únicos",
                        "${metrics.uniqueParticipants}",
                        Icons.Filled.People,
                        Modifier.weight(1f)
                    )
                    MetricCard(
                        "Tasa de asistencia",
                        "${String.format(Locale.US, "%.0f", metrics.attendanceRate)}%",
                        Icons.Filled.TrendingUp,
                        Modifier.weight(1f),
                        SuccessGreen
                    )
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Satisfacción y recomendación", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    String.format(Locale.US, "%.1f", metrics.averageSatisfaction),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    " / 5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RatingStars(
                                rating = metrics.averageSatisfaction.toInt().coerceIn(0, 5),
                                size = 18.dp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ThumbUp, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(5.dp))
                                Text(
                                    "${String.format(Locale.US, "%.0f", metrics.recommendRate)}%",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Text(
                                "recomendaría",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val text = buildReportText(metrics)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Reporte de impacto SyncPass")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Compartir resumen")
                }

                Text("Detalle por evento", style = MaterialTheme.typography.titleLarge)

                if (metrics.events.isEmpty()) {
                    EmptyState(
                        title = "Sin datos en este período",
                        message = "Registra eventos y asistencias para generar el reporte.",
                        icon = Icons.Filled.Assessment
                    )
                } else {
                    metrics.events.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                                .clickable { onEventClick(item.eventId) }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${item.projectName} · ${Dates.formatEventDay(item.startsAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (item.averageRating > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Star,
                                            null,
                                            tint = WarningAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            String.format(Locale.US, "%.1f", item.averageRating),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.attended} asistentes de ${item.enrolled} inscritos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${String.format(Locale.US, "%.0f", item.attendanceRate)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            ThinProgress(value = (item.attendanceRate / 100.0).toFloat())
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Event,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Ver evento",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(0.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderMetric(value: String, label: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = OnBrand,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnBrand.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

private fun buildReportText(metrics: com.rojas.syncpass.data.repository.ImpactMetrics): String =
    buildString {
        appendLine("SyncPass · Reporte de impacto")
        appendLine("Proyectos: ${metrics.totalProjects}")
        appendLine("Eventos: ${metrics.totalEvents}")
        appendLine("Inscripciones: ${metrics.totalEnrollments}")
        appendLine("Asistencias: ${metrics.totalAttendances}")
        appendLine("Participantes únicos: ${metrics.uniqueParticipants}")
        appendLine("Tasa de asistencia: ${String.format(Locale.US, "%.1f", metrics.attendanceRate)}%")
        appendLine("Satisfacción promedio: ${String.format(Locale.US, "%.2f", metrics.averageSatisfaction)}/5")
        appendLine("Recomendación: ${String.format(Locale.US, "%.1f", metrics.recommendRate)}%")
        appendLine()
        metrics.events.forEach { event ->
            appendLine(
                "• ${event.title}: ${event.attended}/${event.enrolled} asistentes, " +
                    "${String.format(Locale.US, "%.0f", event.attendanceRate)}%, " +
                    "satisfacción ${String.format(Locale.US, "%.1f", event.averageRating)}/5"
            )
        }
        appendLine()
        append("Datos ficticios de demostración.")
    }
