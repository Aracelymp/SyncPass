package com.rojas.syncpass.ui.users

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.roleLabel
import com.rojas.syncpass.data.remote.AdminUserCreateRequest
import com.rojas.syncpass.data.remote.AdminUserDto
import com.rojas.syncpass.data.remote.CognitoLinkRequestDto
import com.rojas.syncpass.data.session.Session

private val ROLES = listOf(
    Session.ROLE_COORDINATOR,
    Session.ROLE_STUDENT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(container: AppContainer) {
    val viewModel: UsersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                UsersViewModel(container.userAdminRepository, container.networkMonitor)
            }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeMessage()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var userToToggle by remember { mutableStateOf<AdminUserDto?>(null) }
    var requestToApprove by remember { mutableStateOf<CognitoLinkRequestDto?>(null) }
    var requestToReject by remember { mutableStateOf<CognitoLinkRequestDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de usuarios") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.selectedTab == 0) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear usuario")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Usuarios") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Solicitudes") }
                )
            }

            if (state.isLoading && state.users.isEmpty() && state.requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    0 -> UsersList(
                        users = state.users,
                        isSaving = state.isSaving,
                        onToggle = { userToToggle = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    1 -> RequestsList(
                        requests = state.requests,
                        isSaving = state.isSaving,
                        onApprove = { requestToApprove = it },
                        onReject = { requestToReject = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { request ->
                viewModel.createUser(request) { showCreateDialog = false }
            }
        )
    }

    userToToggle?.let { user ->
        ConfirmActiveDialog(
            user = user,
            onDismiss = { userToToggle = null },
            onConfirm = { active ->
                viewModel.setActive(user, active)
                userToToggle = null
            }
        )
    }

    requestToApprove?.let { request ->
        ApproveRequestDialog(
            request = request,
            users = state.users,
            onDismiss = { requestToApprove = null },
            onApprove = { userId, newUser ->
                viewModel.approveRequest(request, userId, newUser) { requestToApprove = null }
            }
        )
    }

    requestToReject?.let { request ->
        RejectRequestDialog(
            request = request,
            onDismiss = { requestToReject = null },
            onReject = { reason ->
                viewModel.rejectRequest(request, reason) { requestToReject = null }
            }
        )
    }
}

@Composable
private fun UsersList(
    users: List<AdminUserDto>,
    isSaving: Boolean,
    onToggle: (AdminUserDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (users.isEmpty()) {
        EmptyState("No hay usuarios registrados")
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.id }) { user ->
            UserCard(user = user, isSaving = isSaving, onToggle = onToggle)
        }
    }
}

@Composable
private fun UserCard(
    user: AdminUserDto,
    isSaving: Boolean,
    onToggle: (AdminUserDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "@${user.username} · ${user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = roleLabel(user.role),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        text = if (user.active) "Activo" else "Inactivo",
                        positive = user.active
                    )
                    StatusChip(
                        text = if (user.cognitoLinked) "Vinculado Cognito" else "Sin vínculo",
                        positive = user.cognitoLinked
                    )
                }
            }
            IconButton(
                onClick = { onToggle(user) },
                enabled = !isSaving
            ) {
                Icon(
                    imageVector = if (user.active) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = if (user.active) "Desactivar" else "Activar"
                )
            }
        }
    }
}

@Composable
private fun RequestsList(
    requests: List<CognitoLinkRequestDto>,
    isSaving: Boolean,
    onApprove: (CognitoLinkRequestDto) -> Unit,
    onReject: (CognitoLinkRequestDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (requests.isEmpty()) {
        EmptyState("No hay solicitudes de acceso pendientes")
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            RequestCard(
                request = request,
                isSaving = isSaving,
                onApprove = onApprove,
                onReject = onReject
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: CognitoLinkRequestDto,
    isSaving: Boolean,
    onApprove: (CognitoLinkRequestDto) -> Unit,
    onReject: (CognitoLinkRequestDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = request.fullName.ifBlank { request.email },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = request.email,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Solicitud recibida: ${request.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = { onReject(request) },
                    enabled = !isSaving
                ) {
                    Text("Rechazar")
                }
                Button(
                    onClick = { onApprove(request) },
                    enabled = !isSaving
                ) {
                    Text("Aprobar")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, positive: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable(enabled = false) {}
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (AdminUserCreateRequest) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Session.ROLE_STUDENT) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear usuario") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = roleLabel(role),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ROLES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(roleLabel(option)) },
                                onClick = {
                                    role = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        AdminUserCreateRequest(
                            username = username.trim(),
                            email = email.trim(),
                            fullName = fullName.trim(),
                            role = role
                        )
                    )
                },
                enabled = fullName.isNotBlank() && username.isNotBlank() && email.contains("@")
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ConfirmActiveDialog(
    user: AdminUserDto,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val activate = !user.active
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (activate) "Activar usuario" else "Desactivar usuario") },
        text = {
            Text(
                "¿Seguro que quieres ${if (activate) "activar" else "desactivar"} a ${user.fullName}?"
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(activate) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApproveRequestDialog(
    request: CognitoLinkRequestDto,
    users: List<AdminUserDto>,
    onDismiss: () -> Unit,
    onApprove: (Long?, AdminUserCreateRequest?) -> Unit
) {
    val candidates = remember(users) {
        users.filter { it.active && !it.cognitoLinked }
    }
    var isCreatingNew by remember { mutableStateOf(candidates.isEmpty()) }
    var selectedUser by remember { mutableStateOf<AdminUserDto?>(candidates.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf(request.fullName) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(request.email) }
    var role by remember { mutableStateOf(Session.ROLE_STUDENT) }
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aprobar solicitud") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Vincular la identidad de ${request.fullName.ifBlank { request.email }} con:",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (candidates.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isCreatingNew,
                            onClick = { isCreatingNew = false },
                            label = { Text("Existente") }
                        )
                        FilterChip(
                            selected = isCreatingNew,
                            onClick = { isCreatingNew = true },
                            label = { Text("Nuevo") }
                        )
                    }
                }

                if (!isCreatingNew) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedUser?.let { "${it.fullName} (${it.username})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Usuario") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            candidates.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text("${user.fullName} (${user.username})") },
                                    onClick = {
                                        selectedUser = user
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = roleLabel(role),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            ROLES.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(roleLabel(option)) },
                                    onClick = {
                                        role = option
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isCreatingNew) {
                        onApprove(
                            null,
                            AdminUserCreateRequest(
                                username = username.trim(),
                                email = email.trim(),
                                fullName = fullName.trim(),
                                role = role
                            )
                        )
                    } else {
                        onApprove(selectedUser?.id, null)
                    }
                },
                enabled = if (isCreatingNew) {
                    fullName.isNotBlank() && username.isNotBlank() && email.contains("@")
                } else {
                    selectedUser != null
                }
            ) {
                Text("Aprobar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
            }
            label()
        }
    }
}

@Composable
private fun RejectRequestDialog(
    request: CognitoLinkRequestDto,
    onDismiss: () -> Unit,
    onReject: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rechazar solicitud") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Motivo (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onReject(reason) }) {
                Text("Rechazar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
