package com.rojas.syncpass.ui.projects

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.remote.ProjectRequest
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.EmptyState
import com.rojas.syncpass.ui.components.GradientHeader
import com.rojas.syncpass.ui.components.ProjectCard
import com.rojas.syncpass.ui.theme.OnBrand
import java.time.LocalDate

@Composable
fun ProjectsScreen(
    container: AppContainer,
    session: Session,
    onProjectClick: (Long) -> Unit
) {
    val viewModel: ProjectsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProjectsViewModel(
                    container.projectRepository,
                    container.eventRepository,
                    container.networkMonitor
                )
            }
        }
    )
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf("TODOS") }
    var showCreate by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val visible = remember(projects, filter) {
        projects.filter {
            when (filter) {
                "EN_CURSO" -> it.status == "EN_CURSO"
                "FINALIZADO" -> it.status == "FINALIZADO"
                else -> true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GradientHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Proyectos", style = MaterialTheme.typography.headlineMedium, color = OnBrand)
                        Text(
                            "${projects.size} proyectos comunitarios",
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
                            formError = null
                            showCreate = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto", tint = OnBrand)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == "TODOS",
                    onClick = { filter = "TODOS" },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filter == "EN_CURSO",
                    onClick = { filter = "EN_CURSO" },
                    label = { Text("En curso") }
                )
                FilterChip(
                    selected = filter == "FINALIZADO",
                    onClick = { filter = "FINALIZADO" },
                    label = { Text("Finalizados") }
                )
            }

            if (visible.isEmpty()) {
                EmptyState(
                    title = "No hay proyectos para mostrar",
                    message = if (session.isCoordinator) {
                        "Crea el primer proyecto con el botón + del encabezado."
                    } else {
                        "Cuando se publiquen proyectos aparecerán aquí."
                    },
                    icon = Icons.Filled.Folder
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { project ->
                        Column {
                            ProjectCard(project = project, onClick = { onProjectClick(project.id) })
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
                                            editingProject = project
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
                                        onClick = { projectToDelete = project },
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

    if (showCreate) {
        ProjectFormDialog(
            project = null,
            defaultResponsible = session.fullName,
            saving = saving,
            serverError = formError,
            onDismiss = {
                showCreate = false
                formError = null
            },
            onSave = { request ->
                formError = null
                viewModel.create(
                    request,
                    onSuccess = { showCreate = false },
                    onError = { formError = it }
                )
            }
        )
    }

    editingProject?.let { project ->
        ProjectFormDialog(
            project = project,
            defaultResponsible = session.fullName,
            saving = saving,
            serverError = formError,
            onDismiss = {
                editingProject = null
                formError = null
            },
            onSave = { request ->
                formError = null
                viewModel.update(
                    project.id,
                    request,
                    onSuccess = { editingProject = null },
                    onError = { formError = it }
                )
            }
        )
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { if (!saving) projectToDelete = null },
            title = { Text("Eliminar proyecto") },
            text = {
                Text(
                    "¿Eliminar \"${project.name}\"? Esta acción no se puede deshacer. " +
                        "Si el proyecto tiene eventos asociados, no se eliminará."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        projectToDelete = null
                        viewModel.delete(project)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { projectToDelete = null }, enabled = !saving) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProjectFormDialog(
    project: ProjectEntity?,
    defaultResponsible: String,
    saving: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSave: (ProjectRequest) -> Unit
) {
    var code by remember(project?.id) { mutableStateOf(project?.code.orEmpty()) }
    var name by remember(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    var description by remember(project?.id) { mutableStateOf(project?.description.orEmpty()) }
    var startDate by remember(project?.id) {
        mutableStateOf(project?.startDate ?: LocalDate.now().toString())
    }
    var endDate by remember(project?.id) {
        mutableStateOf(project?.endDate ?: LocalDate.now().plusMonths(3).toString())
    }
    var responsible by remember(project?.id) {
        mutableStateOf(project?.responsible ?: defaultResponsible)
    }
    var status by remember(project?.id) { mutableStateOf(project?.status ?: "EN_CURSO") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (project == null) "Nuevo proyecto" else "Editar proyecto") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código (ej. PRY-SALUD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del proyecto") },
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
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Fecha de inicio (AAAA-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Fecha de fin (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = responsible,
                    onValueChange = { responsible = it },
                    label = { Text("Responsable") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == "EN_CURSO",
                        onClick = { status = "EN_CURSO" },
                        label = { Text("En curso") }
                    )
                    FilterChip(
                        selected = status == "PLANIFICADO",
                        onClick = { status = "PLANIFICADO" },
                        label = { Text("Planificado") }
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
                    if (code.isBlank() || name.isBlank() || description.isBlank() ||
                        startDate.isBlank() || endDate.isBlank() || responsible.isBlank()
                    ) {
                        error = "Completa todos los campos obligatorios"
                        return@TextButton
                    }
                    val start = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
                    val end = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()
                    if (start == null || end == null) {
                        error = "Usa fechas válidas con formato AAAA-MM-DD"
                        return@TextButton
                    }
                    if (end.isBefore(start)) {
                        error = "La fecha de fin no puede ser anterior al inicio"
                        return@TextButton
                    }
                    error = null
                    onSave(
                        ProjectRequest(
                            code = code.trim(),
                            name = name.trim(),
                            description = description.trim(),
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            status = status,
                            responsible = responsible.trim()
                        )
                    )
                }
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (project == null) "Crear" else "Guardar")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") }
        }
    )
}
