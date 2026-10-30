package com.rojas.syncpass.ui.events

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.remote.EventRequest
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.EventCard
import com.rojas.syncpass.ui.components.GradientHeader
import com.rojas.syncpass.ui.theme.OnBrand
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(
    container: AppContainer,
    session: Session,
    onEventClick: (Long) -> Unit
) {
    val viewModel: EventsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EventsViewModel(container.eventRepository, container.projectRepository, container.networkMonitor)
            }
        }
    )
    val events by viewModel.events.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("TODOS") }
    var showProjectPicker by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val visible = remember(events, query, filter) {
        events.filter { event ->
            val matchesFilter = when (filter) {
                "PROGRAMADO" -> event.status == "PROGRAMADO" || event.status == "EN_CURSO"
                "FINALIZADO" -> event.status == "FINALIZADO"
                else -> true
            }
            val text = query.trim()
            val matchesQuery = text.isBlank() ||
                event.title.contains(text, ignoreCase = true) ||
                event.projectName.contains(text, ignoreCase = true) ||
                event.location.contains(text, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GradientHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Eventos", style = MaterialTheme.typography.headlineMedium, color = OnBrand)
                        Text(
                            "${events.size} eventos en SyncPass",
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
                    if (session.isCoordinator) {
                        IconButton(onClick = {
                            if (projects.isNotEmpty()) {
                                formError = null
                                showProjectPicker = true
                            } else {
                                viewModel.refresh()
                            }
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Crear evento", tint = OnBrand)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por evento, proyecto o lugar") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == "TODOS",
                    onClick = { filter = "TODOS" },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filter == "PROGRAMADO",
                    onClick = { filter = "PROGRAMADO" },
                    label = { Text("Próximos") }
                )
                FilterChip(
                    selected = filter == "FINALIZADO",
                    onClick = { filter = "FINALIZADO" },
                    label = { Text("Finalizados") }
                )
            }

            if (visible.isEmpty()) {
                EmptyState(
                    title = if (query.isBlank()) "No hay eventos" else "Sin resultados",
                    message = if (query.isBlank()) {
                        "Los eventos publicados por coordinación aparecerán aquí."
                    } else {
                        "Prueba con otro término de búsqueda."
                    },
                    icon = Icons.Filled.Event
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { event ->
                        Column {
                            EventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                showRating = session.isCoordinator
                            )
                            if (session.isCoordinator) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            formError = null
                                            editingEvent = event
                                        },
                                        enabled = !saving
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text("Editar")
                                    }
                                    TextButton(
                                        onClick = { eventToDelete = event },
                                        enabled = !saving
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }

    if (showProjectPicker && session.isCoordinator) {
        AlertDialog(
            onDismissRequest = { if (!saving) showProjectPicker = false },
            title = { Text("Seleccionar proyecto") },
            text = {
                if (projects.isEmpty()) {
                    Text(
                        "No hay proyectos disponibles. Crea un proyecto primero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .height(280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        projects.forEach { project ->
                            OutlinedButton(
                                onClick = {
                                    if (!saving) {
                                        selectedProject = project
                                        showProjectPicker = false
                                        showCreateForm = true
                                    }
                                },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(project.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        project.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProjectPicker = false }, enabled = !saving) {
                    Text("Cancelar")
                }
            }
        )
    }

    val project = selectedProject
    if (showCreateForm && session.isCoordinator && project != null) {
        EventFormDialog(
            projectId = project.id,
            event = null,
            saving = saving,
            serverError = formError,
            onDismiss = {
                if (!saving) {
                    showCreateForm = false
                    selectedProject = null
                    formError = null
                }
            },
            onSave = { request ->
                formError = null
                viewModel.createEvent(
                    request,
                    onSuccess = {
                        showCreateForm = false
                        selectedProject = null
                    },
                    onError = { formError = it }
                )
            }
        )
    }

    editingEvent?.let { event ->
        EventFormDialog(
            projectId = event.projectId,
            event = event,
            saving = saving,
            serverError = formError,
            onDismiss = {
                if (!saving) {
                    editingEvent = null
                    formError = null
                }
            },
            onSave = { request ->
                formError = null
                viewModel.updateEvent(
                    event.id,
                    request,
                    onSuccess = { editingEvent = null },
                    onError = { formError = it }
                )
            }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { if (!saving) eventToDelete = null },
            title = { Text("Eliminar evento") },
            text = {
                Text(
                    "¿Eliminar \"${event.title}\"? Esta acción no se puede deshacer. " +
                        "Si el evento tiene inscripciones, asistencias o encuestas, no se eliminará."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        eventToDelete = null
                        viewModel.deleteEvent(event)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { eventToDelete = null }, enabled = !saving) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private val eventDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

@Composable
private fun EventFormDialog(
    projectId: Long,
    event: EventEntity?,
    saving: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSave: (EventRequest) -> Unit
) {
    val defaultStart = remember {
        LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0)
            .format(eventDateFormatter)
    }
    val defaultEnd = remember {
        LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0)
            .format(eventDateFormatter)
    }
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var description by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var startsAt by remember(event?.id) { mutableStateOf(event?.startsAt ?: defaultStart) }
    var endsAt by remember(event?.id) { mutableStateOf(event?.endsAt ?: defaultEnd) }
    var capacity by remember(event?.id) {
        mutableStateOf((event?.capacity ?: 30).toString())
    }
    var status by remember(event?.id) { mutableStateOf(event?.status ?: "PROGRAMADO") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (event == null) "Nuevo evento" else "Editar evento") },
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
                (error ?: serverError)?.let {
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
                        !LocalDateTime.parse(normalizedEnd).isAfter(LocalDateTime.parse(normalizedStart)) ->
                            error = "La fecha de fin debe ser posterior al inicio"
                        capacityValue == null || capacityValue <= 0 ->
                            error = "La capacidad debe ser mayor que cero"
                        else -> {
                            error = null
                            onSave(
                                EventRequest(
                                    projectId = projectId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    location = location.trim(),
                                    startsAt = normalizedStart,
                                    endsAt = normalizedEnd,
                                    capacity = capacityValue,
                                    qrToken = event?.qrToken,
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
                    Text(if (event == null) "Crear" else "Guardar")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") }
        }
    )
}
