package com.rojas.syncpass.ui.events

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.core.util.eventStatusLabel
import com.rojas.syncpass.core.util.methodLabel
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.DividerSoft
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.GradientButton
import com.rojas.syncpass.ui.components.InfoRow
import com.rojas.syncpass.ui.components.OfflineNote
import com.rojas.syncpass.ui.components.QrPanel
import com.rojas.syncpass.ui.components.RatingStars
import com.rojas.syncpass.ui.components.StatusPill
import com.rojas.syncpass.ui.components.ThinProgress
import com.rojas.syncpass.ui.components.VSpace
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber

@Composable
fun EventDetailScreen(
    container: AppContainer,
    session: Session,
    eventId: Long,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onWalkIn: (Long) -> Unit,
    onSurvey: (Long, String?) -> Unit
) {
    val viewModel: EventDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EventDetailViewModel(
                    eventId,
                    session,
                    container.eventRepository,
                    container.syncRepository,
                    container.database,
                    container.networkMonitor
                )
            }
        }
    )
    val event by viewModel.event.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val enrollments by viewModel.enrollments.collectAsStateWithLifecycle()
    val surveys by viewModel.surveys.collectAsStateWithLifecycle()
    val mySurvey by viewModel.mySurvey.collectAsStateWithLifecycle()
    val eligibleAttendance by viewModel.eligibleAttendance.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (event == null) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Text("Evento", style = MaterialTheme.typography.titleLarge)
                }
                EmptyState(
                    title = "Evento no disponible",
                    message = "Conéctate a internet para descargar este evento o vuelve a la lista de eventos."
                )
            }
        } else {
            EventDetailContent(
                event = event!!,
                attendance = attendance,
                eligibleAttendance = eligibleAttendance,
                surveys = surveys,
                mySurvey = mySurvey,
                isEnrolled = enrollments.any { it.userId == session.userId && it.status == "INSCRITO" },
                enrolledCount = maxOf(event!!.enrolledCount, enrollments.count { it.status == "INSCRITO" }),
                isCoordinator = session.isCoordinator,
                session = session,
                online = online,
                refreshing = refreshing,
                working = working,
                onBack = onBack,
                onRefresh = { viewModel.refresh() },
                onScan = onScan,
                onWalkIn = { onWalkIn(eventId) },
                onSurvey = { attendanceClientId -> onSurvey(eventId, attendanceClientId) },
                onEnroll = viewModel::enroll,
                onCancelEnrollment = viewModel::cancelEnrollment
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }
}

@Composable
private fun EventDetailContent(
    event: EventEntity,
    attendance: List<AttendanceEntity>,
    eligibleAttendance: List<AttendanceEntity>,
    surveys: List<SurveyEntity>,
    mySurvey: SurveyEntity?,
    isEnrolled: Boolean,
    enrolledCount: Int,
    isCoordinator: Boolean,
    session: Session,
    online: Boolean,
    refreshing: Boolean,
    working: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onScan: () -> Unit,
    onWalkIn: () -> Unit,
    onSurvey: (String?) -> Unit,
    onEnroll: () -> Unit,
    onCancelEnrollment: () -> Unit
) {
    var showSurveyPicker by remember { mutableStateOf(false) }
    val myAttendance = attendance.firstOrNull {
        it.userId == session.userId ||
            (it.participantEmail != null && it.participantEmail.equals(session.email, ignoreCase = true))
    }
    val attendedCount = maxOf(event.attendedCount, attendance.size)
    val averageRating = if (surveys.isEmpty()) event.averageRating else surveys.map { it.rating }.average()
    val recommendRate = if (surveys.isEmpty()) 0.0
    else surveys.count { it.wouldRecommend } * 100.0 / surveys.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(BrandGradient)) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = OnBrand)
                    }
                    Text(
                        event.projectName,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnBrand.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onRefresh, enabled = !refreshing) {
                        if (refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = OnBrand
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = OnBrand)
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnBrand,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            contentColor = OnBrand
                        ) {
                            Text(
                                eventStatusLabel(event.status),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp), tint = OnBrand.copy(alpha = 0.85f))
                        Text(
                            Dates.formatEventDate(event.startsAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = OnBrand.copy(alpha = 0.85f))
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        HeaderMetric("Inscritos", "$enrolledCount/${event.capacity}")
                        HeaderMetric("Asistencias", "$attendedCount")
                        if (isCoordinator) {
                            HeaderMetric(
                                "Satisfacción",
                                if (averageRating > 0) String.format("%.1f", averageRating) else "—"
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VSpace(2.dp)

            if (!online) {
                OfflineNote()
            }

            GradientButton(
                text = if (myAttendance == null) "Escanear QR y registrar asistencia" else "Escanear otro participante",
                onClick = onScan,
                icon = Icons.Filled.QrCodeScanner,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onWalkIn,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(8.dp))
                Text("Registrar participante no inscrito")
            }

            if (!isCoordinator && event.status != "CANCELADO") {
                SurfaceCardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isEnrolled) "Estás inscrito en este evento" else "Inscripción al evento",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (isEnrolled) {
                                    "Puedes cancelar tu inscripción si ya no puedes asistir."
                                } else {
                                    "Reserva tu cupo para asegurar tu participación."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isEnrolled) {
                            OutlinedButton(onClick = onCancelEnrollment, enabled = online && !working) {
                                Text("Cancelar")
                            }
                        } else {
                            Button(onClick = onEnroll, enabled = online && !working) {
                                if (working) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Inscribirme")
                                }
                            }
                        }
                    }
                    if (!online) {
                        Text(
                            "Necesitas conexión para inscribirte o cancelar.",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }
            }

            SurfaceCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(
                                if (myAttendance != null) SuccessGreen.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (myAttendance != null) Icons.Filled.CheckCircle else Icons.Filled.Groups,
                            contentDescription = null,
                            tint = if (myAttendance != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (myAttendance != null) "Asistencia registrada" else "Aún no registras asistencia",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (myAttendance != null) {
                                "${Dates.formatInstant(myAttendance.registeredAt)} · ${methodLabel(myAttendance.method)}"
                            } else {
                                "Escanea el código QR del evento para acreditar tu asistencia."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (myAttendance != null) {
                        SyncBadge(myAttendance.syncState)
                    }
                }
            }

            if (myAttendance != null) {
                if (mySurvey != null) {
                    SurfaceCardBlock {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Assignment, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(8.dp))
                            Text("Encuesta de satisfacción enviada", style = MaterialTheme.typography.titleSmall)
                        }
                        if (isCoordinator) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RatingStars(mySurvey.rating, size = 20.dp)
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "General ${mySurvey.rating}/5 · Organización ${mySurvey.organizationRating}/5 · Impacto ${mySurvey.impactRating}/5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            mySurvey.comment?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text(
                                "Tus respuestas son privadas. Solo coordinación puede consultarlas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SyncBadge(if (mySurvey.redacted) "SYNCED" else mySurvey.syncState)
                    }
                } else {
                    SurfaceCardBlock {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Cuéntanos tu experiencia", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Tu opinión ayuda a mejorar los próximos eventos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onSurvey(null) }) { Text("Encuesta") }
                        }
                    }
                }
            }

            SurfaceCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Assignment, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Encuestas asistidas", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (eligibleAttendance.isEmpty()) {
                                "No hay asistencias pendientes de encuesta."
                            } else if (isCoordinator) {
                                "Puedes encuestar cualquier asistencia de este evento."
                            } else {
                                "Solo asistencias que tú registraste o escaneaste."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showSurveyPicker = true },
                    enabled = eligibleAttendance.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Encuestar participante") }
            }

            if (isCoordinator) {
                SurfaceCardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCode2, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Código QR del evento", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Compártelo con los participantes para acreditar asistencia.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        QrPanel(content = com.rojas.syncpass.core.util.Qr.payload(event.id, event.qrToken))
                    }
                    Text(
                        "Token: ${event.qrToken}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SurfaceCardBlock {
                Text("Sobre el evento", style = MaterialTheme.typography.titleSmall)
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DividerSoft()
                InfoRow(
                    Icons.Filled.CalendarMonth,
                    "Fecha y hora",
                    "${Dates.formatEventDate(event.startsAt)} — ${Dates.formatTime(event.endsAt)}"
                )
                InfoRow(Icons.Filled.LocationOn, "Lugar", event.location)
                InfoRow(
                    Icons.Filled.Groups,
                    "Capacidad",
                    "${event.capacity} personas · $enrolledCount inscritos"
                )
                ThinProgress(
                    value = if (event.capacity == 0) 0f
                    else (enrolledCount.toFloat() / event.capacity).coerceIn(0f, 1f)
                )
            }

            if (isCoordinator && surveys.isNotEmpty()) {
                SurfaceCardBlock {
                    Text("Resultados de la encuesta", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SurveyMetric("Respuestas", "${surveys.size}")
                        SurveyMetric("General", String.format("%.1f", surveys.map { it.rating }.average()))
                        SurveyMetric("Organización", String.format("%.1f", surveys.map { it.organizationRating }.average()))
                        SurveyMetric("Impacto", String.format("%.1f", surveys.map { it.impactRating }.average()))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "${String.format("%.0f", recommendRate)}% recomendaría este evento",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    surveys.mapNotNull { it.comment }.take(3).forEach { comment ->
                        Text(
                            "“$comment”",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isCoordinator) {
                SurfaceCardBlock {
                    Text(
                        "Participantes (${attendance.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (attendance.isEmpty()) {
                        Text(
                            "Todavía no hay asistencias registradas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        attendance.forEach { item ->
                            AttendanceRow(item)
                        }
                    }
                }
            }
        }
    }

    if (showSurveyPicker) {
        AlertDialog(
            onDismissRequest = { showSurveyPicker = false },
            title = { Text("Encuestar participante") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Selecciona la asistencia que responderá la encuesta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    eligibleAttendance.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSurveyPicker = false
                                    onSurvey(item.clientId)
                                }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    item.participantName.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.participantName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${methodLabel(item.method)} · ${Dates.formatTime(item.registeredAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurveyPicker = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HeaderMetric(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
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
private fun SurfaceCardBlock(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SyncBadge(state: String) {
    val synced = state == "SYNCED"
    StatusPill(
        text = if (synced) "Sincronizado" else if (state == "ERROR") "Error" else "Pendiente",
        color = if (synced) SuccessGreen else WarningAmber
    )
}

@Composable
private fun SurveyMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttendanceRow(item: AttendanceEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.participantName.trim().take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.participantName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${methodLabel(item.method)} · ${Dates.formatTime(item.registeredAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (item.syncState == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
            contentDescription = null,
            tint = if (item.syncState == "SYNCED") SuccessGreen else WarningAmber,
            modifier = Modifier.size(18.dp)
        )
    }
}
