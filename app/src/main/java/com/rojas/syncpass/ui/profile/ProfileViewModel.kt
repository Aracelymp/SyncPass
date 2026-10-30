package com.rojas.syncpass.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.local.PendingOperationEntity
import com.rojas.syncpass.data.repository.SessionRepository
import com.rojas.syncpass.data.repository.SyncOutcome
import com.rojas.syncpass.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val online: StateFlow<Boolean> = networkMonitor.online
    val pendingCount = syncRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val lastSync = syncRepository.lastSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pendingOperations: StateFlow<List<PendingOperationEntity>> = syncRepository.pendingOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun syncNow() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _message.value = when (val outcome = syncRepository.syncNow()) {
                is SyncOutcome.Success ->
                    "Sincronización completa: ${outcome.pushed} enviados, ${outcome.duplicates} duplicados, ${outcome.downloaded} descargados."
                SyncOutcome.Offline -> "Sin conexión. Los registros quedan pendientes."
                SyncOutcome.OfflineSession ->
                    "Sesión local. Cierra sesión e ingresa nuevamente con internet para sincronizar."
                SyncOutcome.NoSession -> "No hay una sesión activa."
                is SyncOutcome.Error -> "No se pudo sincronizar: ${outcome.message}"
            }
            _busy.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _busy.value = true
            sessionRepository.logout()
            _busy.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
