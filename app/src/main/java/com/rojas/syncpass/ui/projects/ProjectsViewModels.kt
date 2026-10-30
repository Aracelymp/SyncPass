package com.rojas.syncpass.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.remote.EventRequest
import com.rojas.syncpass.data.remote.ProjectRequest
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = networkMonitor.online

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh(notify = false)
    }

    fun refresh(notify: Boolean = true) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            if (networkMonitor.isCurrentlyOnline()) {
                projectRepository.refresh()
                    .onFailure { if (notify) _message.value = it.message }
                eventRepository.refresh()
                    .onFailure { if (notify) _message.value = it.message }
            } else if (notify) {
                _message.value = "Sin conexión. Se muestran los proyectos guardados."
            }
            _refreshing.value = false
        }
    }

    fun create(
        request: ProjectRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            val message = "Sin conexión. Conéctate para crear proyectos."
            _message.value = message
            onError(message)
            return
        }
        viewModelScope.launch {
            _saving.value = true
            projectRepository.create(request)
                .onSuccess {
                    _message.value = "Proyecto creado: ${it.name}"
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message
                    onError(it.message ?: "No se pudo crear el proyecto")
                }
            _saving.value = false
        }
    }

    fun update(
        id: Long,
        request: ProjectRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            _message.value = "Sin conexión. Conéctate para editar proyectos."
            onError("Sin conexión. Conéctate para editar proyectos.")
            return
        }
        viewModelScope.launch {
            _saving.value = true
            projectRepository.update(id, request)
                .onSuccess {
                    _message.value = "Proyecto actualizado: ${it.name}"
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message
                    onError(it.message ?: "No se pudo actualizar el proyecto")
                }
            _saving.value = false
        }
    }

    fun delete(project: ProjectEntity) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            _message.value = "Sin conexión. Conéctate para eliminar proyectos."
            return
        }
        viewModelScope.launch {
            _saving.value = true
            projectRepository.delete(project.id)
                .onSuccess { _message.value = "$it: ${project.name}" }
                .onFailure { _message.value = it.message }
            _saving.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

class ProjectDetailViewModel(
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val project: StateFlow<ProjectEntity?> = projectRepository.project(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<EventEntity>> = eventRepository.eventsForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = networkMonitor.online

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh(notify = false)
    }

    fun refresh(notify: Boolean = true) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            if (networkMonitor.isCurrentlyOnline()) {
                projectRepository.refresh()
                eventRepository.refresh()
                    .onFailure { if (notify) _message.value = it.message }
            } else if (notify) {
                _message.value = "Sin conexión. Se muestran los datos guardados."
            }
            _refreshing.value = false
        }
    }

    fun createEvent(request: EventRequest, onSuccess: () -> Unit) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            eventRepository.create(request)
                .onSuccess {
                    _message.value = "Evento creado: ${it.title}"
                    onSuccess()
                }
                .onFailure { _message.value = it.message }
            _saving.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
