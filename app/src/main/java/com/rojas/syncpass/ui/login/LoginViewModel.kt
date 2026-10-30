package com.rojas.syncpass.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.repository.AuthRepository
import com.rojas.syncpass.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the login form state and delegates authentication to AuthRepository.
 * Online: Cognito direct auth. Offline: locally cached credential verification.
 */
class LoginViewModel(
    private val repository: AuthRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val online: StateFlow<Boolean> = networkMonitor.online

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null, loginError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, loginError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, loginError = null) }
    }

    fun onConfirmNewPasswordChange(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, loginError = null) }
    }

    fun onCompleteNewPasswordClick() {
        val current = _uiState.value

        if (current.newPassword.length < 8) {
            _uiState.update {
                it.copy(loginError = "La nueva contraseña debe tener al menos 8 caracteres.")
            }
            return
        }

        if (current.newPassword != current.confirmNewPassword) {
            _uiState.update {
                it.copy(loginError = "Las contraseñas nuevas no coinciden.")
            }
            return
        }

        val challengeSession = current.challengeSession
        val challengeUsername = current.challengeUsername

        if (challengeSession.isNullOrBlank() || challengeUsername.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isNewPasswordRequired = false,
                    loginError = "La sesión de cambio de contraseña expiró. Inicia sesión nuevamente."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loginError = null) }

        viewModelScope.launch {
            when (
                val result = repository.completeCognitoNewPassword(
                    username = challengeUsername,
                    newPassword = current.newPassword,
                    challengeSession = challengeSession
                )
            ) {
                is LoginResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isSuccess = true)
                }

                is LoginResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, loginError = result.message)
                }

                is LoginResult.IdentityNotLinked -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isIdentityNotLinked = true,
                        pendingIdToken = result.idToken
                    )
                }

                is LoginResult.NewPasswordRequired -> _uiState.update {
                    it.copy(isLoading = false, loginError = "Cognito todavía requiere cambiar la contraseña.")
                }
            }
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val current = _uiState.value

        val usernameError =
            if (current.username.isBlank()) "Ingresa tu usuario o correo" else null
        val passwordError =
            if (current.password.length < 6) "Mínimo 6 caracteres" else null

        if (usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(usernameError = usernameError, passwordError = passwordError)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loginError = null) }

        viewModelScope.launch {
            val result = if (online.value) {
                repository.loginWithCognito(current.username, current.password)
            } else {
                repository.login(current.username, current.password)
            }

            when (result) {
                is LoginResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isSuccess = true)
                }

                is LoginResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, loginError = result.message)
                }

                is LoginResult.IdentityNotLinked -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isIdentityNotLinked = true,
                        pendingIdToken = result.idToken
                    )
                }

                is LoginResult.NewPasswordRequired -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNewPasswordRequired = true,
                        challengeSession = result.session,
                        challengeUsername = result.username,
                        newPassword = "",
                        confirmNewPassword = "",
                        loginError = null
                    )
                }
            }
        }
    }

    fun requestAccess() {
        val idToken = _uiState.value.pendingIdToken
        if (idToken.isBlank()) {
            _uiState.update { it.copy(accessRequestError = "Token de Cognito no disponible") }
            return
        }
        _uiState.update { it.copy(isLoading = true, accessRequestError = null, accessRequestMessage = null) }
        viewModelScope.launch {
            repository.requestCognitoAccess(idToken)
                .onSuccess { request ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestMessage = "Solicitud enviada. Estado: ${request.status}. Espera aprobación de coordinación.",
                            accessRequestError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accessRequestError = error.message,
                            accessRequestMessage = null
                        )
                    }
                }
        }
    }

    fun resetIdentityNotLinked() {
        _uiState.update {
            it.copy(
                isIdentityNotLinked = false,
                pendingIdToken = "",
                accessRequestMessage = null,
                accessRequestError = null,
                loginError = null
            )
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(loginError = null) }
    }

    fun clearAccessRequestMessage() {
        _uiState.update { it.copy(accessRequestMessage = null, accessRequestError = null) }
    }
}
