package com.rojas.syncpass.ui.walkin

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.core.util.Dates
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber

@Composable
fun WalkInScreen(
    container: AppContainer,
    session: Session,
    eventId: Long,
    onBack: () -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    val viewModel: WalkInViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                WalkInViewModel(
                    eventId,
                    session,
                    container.attendanceRepository,
                    container.eventRepository
                )
            }
        }
    )
    val event by viewModel.event.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(BrandGradient)) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = OnBrand)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Registrar participante",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnBrand
                        )
                        Text(
                            event?.title ?: "Evento",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBrand.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (form.success != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Text("Asistencia registrada", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        form.success!!.participantName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        Dates.formatInstant(form.success!!.registeredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (form.success!!.syncState == "SYNCED") {
                            "Sincronizado con el servidor"
                        } else {
                            "Guardado localmente. Se sincronizará automáticamente."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (form.success!!.syncState == "SYNCED") SuccessGreen else WarningAmber
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Registrar otro") }
                        Button(
                            onClick = { onOpenEvent(eventId) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Ver evento") }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Este registro es para personas que llegaron al evento sin inscripción previa. Funciona sin conexión y se sincroniza después.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = form.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nombre completo *") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    isError = form.error != null && form.name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Correo electrónico (opcional)") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.document,
                    onValueChange = viewModel::onDocumentChange,
                    label = { Text("Documento de identidad (opcional)") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                form.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = viewModel::submit,
                    enabled = !form.saving,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    if (form.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Registrar asistencia")
                    }
                }
            }
        }
    }
}
