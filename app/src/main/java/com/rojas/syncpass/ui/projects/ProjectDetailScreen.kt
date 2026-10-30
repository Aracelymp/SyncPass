package com.rojas.syncpass.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.core.util.projectStatusLabel
import com.rojas.syncpass.data.remote.EventRequest
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.EventCard
import com.rojas.syncpass.ui.components.StatusPill
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ProjectDetailScreen(
    container: AppContainer,
    session: Session,
    projectId: Long,
    onBack: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    val viewModel: ProjectDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProjectDetailViewModel(
                    projectId,
                    container.projectRepository,
                    container.eventRepository,
                    container.networkMonitor
                )
            }
        }
    )
    val project by viewModel.project.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(BrandGradient)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = OnBrand)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                project?.code ?: "Proyecto",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnBrand.copy(alpha = 0.72f)
                            )
                            Text(
                                project?.name ?: "Cargando proyecto…",
                                style = MaterialTheme.typography.titleLarge,
                                color = OnBrand
                            )
                        }
                        IconButton(onClick = { viewModel.refresh() }, enabled = !refreshing) {
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
                        if (session.isCoordinator) {
                            IconButton(onClick = { showCreate = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Nuevo evento", tint = OnBrand)
                            }
                        }
                    }
                    project?.let { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f),
                                    contentColor = OnBrand
                                ) {
                                    Text(
                                        projectStatusLabel(item.status),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnBrand.copy(alpha = 0.9f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(15.dp), tint = OnBrand.copy(alpha = 0.8f))
                                Text(
                                    "${Dates.formatDate(item.startDate)} — ${Dates.formatDate(item.endDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnBrand.copy(alpha = 0.82f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Person, null, Modifier.size(15.dp), tint = OnBrand.copy(alpha = 0.8f))
                                Text(
                                    "Responsable: ${item.responsible}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnBrand.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Eventos del proyecto",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "${events.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (events.isEmpty()) {
                EmptyState(
                    title = "Aún no hay eventos",
                    message = if (session.isCoordinator) {
                        "Crea un evento con el botón + del encabezado."
                    } else {
                        "Cuando se programen eventos aparecerán aquí."
                    },
                    icon = Icons.Filled.Event
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            showRating = session.isCoordinator
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }

    if (showCreate) {
        EventFormDialog(
            projectId = projectId,
            saving = saving,
            onDismiss = { showCreate = false },
            onCreate = { request -> viewModel.createEvent(request) { showCreate = false } }
        )
    }
}

private val eventDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

@Composable
private fun EventFormDialog(
    projectId: Long,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (EventRequest) -> Unit
) {
    val defaultStart = remember {
        LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0)
            .format(eventDateFormatter)
    }
    val defaultEnd = remember {
        LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0)
            .format(eventDateFormatter)
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startsAt by remember { mutableStateOf(defaultStart) }
    var endsAt by remember { mutableStateOf(defaultEnd) }
    var capacity by remember { mutableStateOf("30") }
    var status by remember { mutableStateOf("PROGRAMADO") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Nuevo evento") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lugar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startsAt,
                    onValueChange = { startsAt = it },
                    label = { Text("Inicio (AAAA-MM-DDTHH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endsAt,
                    onValueChange = { endsAt = it },
                    label = { Text("Fin (AAAA-MM-DDTHH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter(Char::isDigit) },
                    label = { Text("Capacidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == "PROGRAMADO",
                        onClick = { status = "PROGRAMADO" },
                        label = { Text("Programado") }
                    )
                    FilterChip(
                        selected = status == "EN_CURSO",
                        onClick = { status = "EN_CURSO" },
                        label = { Text("En curso") }
                    )
                    FilterChip(
                        selected = status == "FINALIZADO",
                        onClick = { status = "FINALIZADO" },
                        label = { Text("Finalizado") }
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val normalizedStart = Dates.normalizeInputDate(startsAt)
                    val normalizedEnd = Dates.normalizeInputDate(endsAt)
                    val capacityValue = capacity.toIntOrNull()
                    when {
                        title.isBlank() || description.isBlank() || location.isBlank() ->
                            error = "Completa título, descripción y lugar"
                        normalizedStart == null || normalizedEnd == null ->
                            error = "Usa el formato AAAA-MM-DDTHH:mm en las fechas"
                        capacityValue == null || capacityValue <= 0 ->
                            error = "La capacidad debe ser mayor que cero"
                        else -> {
                            error = null
                            onCreate(
                                EventRequest(
                                    projectId = projectId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    location = location.trim(),
                                    startsAt = normalizedStart,
                                    endsAt = normalizedEnd,
                                    capacity = capacityValue,
                                    qrToken = null,
                                    status = status
                                )
                            )
                        }
                    }
                }
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") }
        }
    )
}
