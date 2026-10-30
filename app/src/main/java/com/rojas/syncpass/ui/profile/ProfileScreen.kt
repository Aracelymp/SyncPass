package com.rojas.syncpass.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.BuildConfig
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.config.AppConfig
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.Avatar
import com.rojas.syncpass.ui.components.DividerSoft
import com.rojas.syncpass.ui.components.GradientHeader
import com.rojas.syncpass.ui.components.InfoRow
import com.rojas.syncpass.ui.components.StatusPill
import com.rojas.syncpass.ui.components.SyncPill
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber

@Composable
fun ProfileScreen(
    container: AppContainer,
    session: Session
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    container.sessionRepository,
                    container.syncRepository,
                    container.networkMonitor
                )
            }
        }
    )
    val online by viewModel.online.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()
    val pendingOperations by viewModel.pendingOperations.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var confirmLogout by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            GradientHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(session.fullName, size = 64.dp)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            session.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnBrand
                        )
                        Text(
                            session.roleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.82f)
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = if (session.offline) "Sesión local" else "Sesión verificada",
                                color = if (session.offline) WarningAmber else SuccessGreen
                            )
                        }
                    }
                }
            }

            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.size(2.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text("Cuenta", style = MaterialTheme.typography.titleSmall)
                    InfoRow(Icons.Filled.Person, "Nombre", session.fullName)
                    InfoRow(Icons.Filled.AlternateEmail, "Usuario y correo", "${session.username} · ${session.email}")
                    InfoRow(Icons.Filled.Badge, "Rol", session.roleLabel)
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sincronización", style = MaterialTheme.typography.titleSmall)
                        SyncPill(online = online, pending = pending)
                    }
                    Text(
                        "Última sincronización: ${Dates.relativeLastSync(lastSync)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (pending > 0) WarningAmber.copy(alpha = 0.1f)
                                else SuccessGreen.copy(alpha = 0.09f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                !online -> Icons.Filled.CloudOff
                                pending > 0 -> Icons.Filled.CloudUpload
                                else -> Icons.Filled.CloudDone
                            },
                            contentDescription = null,
                            tint = if (pending > 0 || !online) WarningAmber else SuccessGreen
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    !online -> "Trabajando sin conexión"
                                    pending > 0 -> "$pending operaciones pendientes"
                                    else -> "Todo sincronizado"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                when {
                                    !online -> "Los registros se guardan en el dispositivo."
                                    pending > 0 -> "Se enviarán automáticamente al recuperar conexión."
                                    else -> "Los datos locales coinciden con el servidor."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (session.offline) {
                        Text(
                            "Iniciaste sesión sin conexión. Para enviar los registros pendientes cierra sesión y vuelve a ingresar con internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber
                        )
                    }
                    Button(
                        onClick = viewModel::syncNow,
                        enabled = online && !session.offline && !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Sincronizar ahora")
                        }
                    }
                    Text(
                        "La sincronización automática se ejecuta al recuperar conexión, al iniciar sesión y cada 15 minutos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (pendingOperations.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cola de sincronización", style = MaterialTheme.typography.titleSmall)
                        pendingOperations.take(6).forEach { operation ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when (operation.type) {
                                            "ATTENDANCE_QR" -> "Asistencia por QR"
                                            "ATTENDANCE_WALK_IN" -> "Participante no inscrito"
                                            "SURVEY" -> "Encuesta de satisfacción"
                                            else -> operation.type
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        Dates.formatInstant(operation.occurredAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = if (operation.attempts > 0) "Reintentando" else "Pendiente",
                                    color = WarningAmber
                                )
                            }
                        }
                        DividerSoft()
                        Text(
                            "Los pendientes se envían en lote cuando haya conexión y una sesión válida.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text("Aplicación", style = MaterialTheme.typography.titleSmall)
                    InfoRow(Icons.Filled.CloudSync, "Servidor", AppConfig.apiBaseUrl)
                    InfoRow(Icons.Filled.Storage, "Almacenamiento", "Room / SQLite + outbox offline")
                    InfoRow(Icons.Filled.Insights, "Versión", "SyncPass ${BuildConfig.VERSION_NAME}")
                    InfoRow(Icons.Filled.Info, "Datos", "Ficticios, solo para demostración")
                }

                OutlinedButton(
                    onClick = { confirmLogout = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Cerrar sesión")
                }

                Text(
                    "SyncPass · Proyecto académico\nAsistencia, participación e impacto comunitario",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Cerrar sesión") },
            text = {
                Text(
                    "Se eliminarán del dispositivo las asistencias y encuestas locales. " +
                        "Los registros aún pendientes podrían perderse si no se han sincronizado."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        viewModel.logout()
                    }
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            }
        )
    }
}
