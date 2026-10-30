package com.rojas.syncpass.ui.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.ui.components.AuthCard
import com.rojas.syncpass.ui.components.BrandMark
import com.rojas.syncpass.ui.components.GradientButton
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand

@Composable
fun LoginScreen(
    container: AppContainer,
    onLoginSuccess: () -> Unit = {}
) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LoginViewModel(container.authRepository, container.networkMonitor)
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()

    LoginContent(
        state = state,
        isOnline = online,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmNewPasswordChange = viewModel::onConfirmNewPasswordChange,
        onTogglePassword = viewModel::onTogglePasswordVisibility,
        onLoginClick = viewModel::onLoginClick,
        onCompleteNewPasswordClick = viewModel::onCompleteNewPasswordClick,
        onRequestAccessClick = viewModel::requestAccess,
        onResetIdentityNotLinked = viewModel::resetIdentityNotLinked,
        onErrorShown = viewModel::onErrorShown,
        onLoginSuccess = onLoginSuccess
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    isOnline: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLoginClick: () -> Unit,
    onCompleteNewPasswordClick: () -> Unit,
    onRequestAccessClick: () -> Unit,
    onResetIdentityNotLinked: () -> Unit,
    onErrorShown: () -> Unit,
    onLoginSuccess: () -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.loginError) {
        val message = state.loginError ?: return@LaunchedEffect
        snackbar.showSnackbar(
            message = message,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        onErrorShown()
    }
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            BrandMark(size = 74.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "SyncPass",
                style = MaterialTheme.typography.displaySmall,
                color = OnBrand
            )
            Text(
                "Asistencia, participación e impacto en un solo lugar",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBrand.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp)
            )
            Spacer(Modifier.height(22.dp))

            NetworkIndicator(isOnline)
            Spacer(Modifier.height(18.dp))

            AuthCard(
                title = when {
                    state.isIdentityNotLinked -> "Cuenta no autorizada"
                    state.isNewPasswordRequired -> "Cambia tu contraseña"
                    else -> "Inicia sesión"
                },
                subtitle = when {
                    state.isIdentityNotLinked ->
                        "Tu identidad de Cognito no está vinculada a SyncPass"
                    state.isNewPasswordRequired ->
                        "Cognito requiere que establezcas una nueva contraseña antes de continuar"
                    isOnline ->
                        "Ingresa tu usuario y contraseña institucionales"
                    else ->
                        "Puedes entrar con una cuenta que ya haya iniciado sesión en este dispositivo"
                }
            ) {
                when {
                    state.isIdentityNotLinked -> IdentityNotLinkedForm(
                        state = state,
                        onRequestAccess = onRequestAccessClick,
                        onBack = onResetIdentityNotLinked
                    )

                    state.isNewPasswordRequired -> NewPasswordForm(
                        state = state,
                        onNewPasswordChange = onNewPasswordChange,
                        onConfirmNewPasswordChange = onConfirmNewPasswordChange,
                        onComplete = onCompleteNewPasswordClick
                    )

                    else -> LoginForm(
                        state = state,
                        isOnline = isOnline,
                        onUsernameChange = onUsernameChange,
                        onPasswordChange = onPasswordChange,
                        onTogglePassword = onTogglePassword,
                        onLoginClick = onLoginClick
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(12.dp)
        )
    }
}

@Composable
private fun NetworkIndicator(isOnline: Boolean) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
            contentDescription = null,
            tint = OnBrand,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (isOnline) "Conectado al servidor" else "Sin conexión · acceso local",
            style = MaterialTheme.typography.labelSmall,
            color = OnBrand
        )
    }
}

@Composable
private fun LoginForm(
    state: LoginUiState,
    isOnline: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLoginClick: () -> Unit
) {
    OutlinedTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        label = { Text("Usuario o correo") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        isError = state.usernameError != null,
        supportingText = { state.usernameError?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text("Contraseña") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        isError = state.passwordError != null,
        supportingText = { state.passwordError?.let { Text(it) } },
        visualTransformation = if (state.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector = if (state.isPasswordVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (state.isPasswordVisible) {
                        "Ocultar contraseña"
                    } else {
                        "Mostrar contraseña"
                    }
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
        modifier = Modifier.fillMaxWidth()
    )

    GradientButton(
        text = if (state.isLoading) "Ingresando…" else "Ingresar",
        onClick = onLoginClick,
        enabled = state.canSubmit && !state.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    )

    if (state.isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun NewPasswordForm(
    state: LoginUiState,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Text(
        "Nueva contraseña para ${state.challengeUsername ?: state.username}",
        style = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
        value = state.newPassword,
        onValueChange = onNewPasswordChange,
        label = { Text("Nueva contraseña") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = state.confirmNewPassword,
        onValueChange = onConfirmNewPasswordChange,
        label = { Text("Confirmar nueva contraseña") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onComplete() }),
        modifier = Modifier.fillMaxWidth()
    )

    GradientButton(
        text = if (state.isLoading) "Guardando…" else "Cambiar contraseña",
        onClick = onComplete,
        enabled = state.canSubmitNewPassword,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    )

    if (state.isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun IdentityNotLinkedForm(
    state: LoginUiState,
    onRequestAccess: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Solo los usuarios autorizados por coordinación pueden acceder. " +
                "Si ya tienes una cuenta de Cognito, envía una solicitud de acceso.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        state.accessRequestMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        state.accessRequestError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onRequestAccess,
            enabled = state.pendingIdToken.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isLoading) "Enviando…" else "Solicitar acceso")
        }

        TextButton(onClick = onBack) {
            Text("Volver al inicio de sesión")
        }
    }
}
