package com.rojas.syncpass.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.remote.AdminUserCreateRequest
import com.rojas.syncpass.data.remote.AdminUserDto
import com.rojas.syncpass.data.remote.CognitoLinkRequestDto
import com.rojas.syncpass.data.repository.UserAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsersViewModel(
    private val repository: UserAdminRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            if (!networkMonitor.isCurrentlyOnline()) {
                _uiState.update { it.copy(isLoading = false, message = "Sin conexión. Conéctate para administrar usuarios.") }
                return@launch
            }
            val usersResult = repository.getUsers()
            val requestsResult = repository.getPendingRequests()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    users = usersResult.getOrElse { emptyList() },
                    requests = requestsResult.getOrElse { emptyList() },
                    message = usersResult.exceptionOrNull()?.message
                        ?: requestsResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun createUser(request: AdminUserCreateRequest, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            repository.createUser(request)
                .onSuccess { user ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            users = state.users + user,
                            message = "Usuario creado: ${user.fullName}"
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message) }
                }
        }
    }

    fun setActive(user: AdminUserDto, active: Boolean) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            repository.setActive(user.id, active)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            users = state.users.map { if (it.id == updated.id) updated else it },
                            message = "${updated.fullName} ${if (updated.active) "activado" else "desactivado"}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message) }
                }
        }
    }

    fun approveRequest(
        request: CognitoLinkRequestDto,
        userId: Long?,
        newUser: AdminUserCreateRequest?,
        onSuccess: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            repository.approveRequest(request.id, userId, newUser)
                .onSuccess { approved ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            requests = state.requests.filter { it.id != approved.id },
                            users = state.users.map { user ->
                                if (user.id == approved.linkedUserId) user.copy(cognitoLinked = true) else user
                            },
                            message = "Solicitud aprobada para ${approved.linkedUsername ?: approved.email}"
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message) }
                }
        }
    }

    fun rejectRequest(request: CognitoLinkRequestDto, reason: String, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            repository.rejectRequest(request.id, reason)
                .onSuccess { rejected ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            requests = state.requests.filter { it.id != rejected.id },
                            message = "Solicitud rechazada"
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, message = error.message) }
                }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class UsersUiState(
    val selectedTab: Int = 0,
    val users: List<AdminUserDto> = emptyList(),
    val requests: List<CognitoLinkRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)
