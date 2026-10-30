package com.rojas.syncpass.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.ProjectEntity
import com.rojas.syncpass.data.remote.EventRequest
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val projectRepository: ProjectRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val events: StateFlow<List<EventEntity>> = eventRepository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                _message.value = "Sin conexión. Se muestran los eventos guardados."
            }
            _refreshing.value = false
        }
    }

    fun createEvent(
        request: EventRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            val message = "Sin conexión. Conéctate para crear eventos."
            _message.value = message
            onError(message)
            return
        }
        viewModelScope.launch {
            _saving.value = true
            eventRepository.create(request)
                .onSuccess {
                    _message.value = "Evento creado: ${it.title}"
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message
                    onError(it.message ?: "No se pudo crear el evento")
                }
            _saving.value = false
        }
    }

    fun updateEvent(
        id: Long,
        request: EventRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            val message = "Sin conexión. Conéctate para editar eventos."
            _message.value = message
            onError(message)
            return
        }
        viewModelScope.launch {
            _saving.value = true
            eventRepository.update(id, request)
                .onSuccess {
                    _message.value = "Evento actualizado: ${it.title}"
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message
                    onError(it.message ?: "No se pudo actualizar el evento")
                }
            _saving.value = false
        }
    }

    fun deleteEvent(event: EventEntity) {
        if (_saving.value) return
        if (!networkMonitor.isCurrentlyOnline()) {
            _message.value = "Sin conexión. Conéctate para eliminar eventos."
            return
        }
        viewModelScope.launch {
            _saving.value = true
            eventRepository.delete(event.id)
                .onSuccess { _message.value = "$it: ${event.title}" }
                .onFailure { _message.value = it.message }
            _saving.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
