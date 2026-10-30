package com.rojas.syncpass.ui.survey

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.ui.components.RatingInput
import com.rojas.syncpass.ui.components.RatingStars
import com.rojas.syncpass.ui.theme.BrandGradient
import com.rojas.syncpass.ui.theme.OnBrand
import com.rojas.syncpass.ui.theme.SuccessGreen
import com.rojas.syncpass.ui.theme.WarningAmber

@Composable
fun SurveyScreen(
    container: AppContainer,
    session: Session,
    eventId: Long,
    attendanceClientId: String?,
    onBack: () -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    val viewModel: SurveyViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SurveyViewModel(
                    eventId,
                    session,
                    attendanceClientId,
                    container.eventRepository,
                    container.surveyRepository,
                    container.database.attendanceDao()
                )
            }
        }
    )
    val event by viewModel.event.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
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
                            if (viewModel.assisted) "Encuesta a participante" else "Encuesta de satisfacción",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnBrand
                        )
                        Text(
                            if (viewModel.assisted) {
                                attendance?.participantName ?: event?.title ?: "Evento"
                            } else {
                                event?.title ?: "Evento"
                            },
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
            if (form.submitted != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Text("¡Gracias por tu opinión!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (viewModel.assisted) {
                            "La encuesta quedó guardada y se sincronizará automáticamente."
                        } else {
                            "Tu encuesta quedó guardada y se sincronizará automáticamente."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.isCoordinator) {
                        RatingStars(form.submitted!!.rating, size = 26.dp)
                    } else {
                        Text(
                            "Solo coordinación puede consultar las respuestas y comentarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f)
                        ) { Text("Volver") }
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
                    Icon(Icons.Filled.Star, null, tint = WarningAmber)
                    Text(
                        if (viewModel.assisted) {
                            "Encuesta asistida para ${attendance?.participantName ?: "el participante"}. " +
                                "El monitor no podrá consultar las respuestas; solo coordinación."
                        } else {
                            "Califica tu experiencia. La información se usa para reportes de impacto anónimos y agregados."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Satisfacción general", style = MaterialTheme.typography.titleSmall)
                    RatingInput(
                        label = "¿Cómo calificas el evento?",
                        value = form.general,
                        onValueChange = viewModel::onGeneralChange
                    )
                    RatingInput(
                        label = "Organización y logística",
                        value = form.organization,
                        onValueChange = viewModel::onOrganizationChange
                    )
                    RatingInput(
                        label = "Impacto en la comunidad",
                        value = form.impact,
                        onValueChange = viewModel::onImpactChange
                    )
                }

                OutlinedTextField(
                    value = form.comment,
                    onValueChange = viewModel::onCommentChange,
                    label = { Text("Comentarios (opcional)") },
                    placeholder = { Text("¿Qué te gustó? ¿Qué podríamos mejorar?") },
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${form.comment.length}/600") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("¿Recomendarías este evento?", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Comparte tu recomendación con otros voluntarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = form.wouldRecommend,
                        onCheckedChange = viewModel::onRecommendChange
                    )
                }

                form.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = viewModel::submit,
                    enabled = !form.saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (form.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Enviar encuesta")
                    }
                }
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}
