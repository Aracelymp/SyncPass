package com.rojas.syncpass.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.repository.DashboardRepository
import com.rojas.syncpass.data.repository.DashboardStats
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.ProjectRepository
import com.rojas.syncpass.data.repository.SyncOutcome
import com.rojas.syncpass.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    dashboardRepository: DashboardRepository,
    private val projectRepository: ProjectRepository,
    private val eventRepository: EventRepository,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val stats: StateFlow<DashboardStats?> = dashboardRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val online: StateFlow<Boolean> = networkMonitor.online
    val lastSync = syncRepository.lastSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pendingCount = syncRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val pendingOperations = syncRepository.pendingOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

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
                when (val outcome = syncRepository.syncNow()) {
                    is SyncOutcome.Success -> if (notify) {
                        _message.value = "Sincronización completa: ${outcome.pushed} enviados, " +
                            "${outcome.duplicates} duplicados, ${outcome.downloaded} descargados."
                    }
                    SyncOutcome.Offline -> if (notify) {
                        _message.value = "Sin conexión. Se muestran los datos guardados."
                    }
                    SyncOutcome.OfflineSession -> if (notify) {
                        _message.value = "Sesión local. Vuelve a iniciar sesión con internet para sincronizar."
                    }
                    SyncOutcome.NoSession -> Unit
                    is SyncOutcome.Error -> if (notify) {
                        _message.value = "No se pudo sincronizar: ${outcome.message}"
                    }
                }
            } else if (notify) {
                _message.value = "Sin conexión. Se muestran los datos guardados."
            }
            _refreshing.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
