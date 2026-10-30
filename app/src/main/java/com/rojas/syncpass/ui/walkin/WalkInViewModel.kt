package com.rojas.syncpass.ui.walkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.repository.AttendanceRepository
import com.rojas.syncpass.data.repository.AttendanceResult
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalkInFormState(
    val name: String = "",
    val email: String = "",
    val document: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val success: AttendanceEntity? = null
)

class WalkInViewModel(
    private val eventId: Long,
    private val session: Session,
    private val attendanceRepository: AttendanceRepository,
    eventRepository: EventRepository
) : ViewModel() {

    val event: StateFlow<EventEntity?> = eventRepository.event(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _form = MutableStateFlow(WalkInFormState())
    val form: StateFlow<WalkInFormState> = _form.asStateFlow()

    fun onNameChange(value: String) {
        _form.value = _form.value.copy(name = value, error = null)
    }

    fun onEmailChange(value: String) {
        _form.value = _form.value.copy(email = value, error = null)
    }

    fun onDocumentChange(value: String) {
        _form.value = _form.value.copy(document = value, error = null)
    }

    fun submit() {
        val current = _form.value
        if (current.saving || current.success != null) return
        if (current.name.isBlank()) {
            _form.value = current.copy(error = "Ingresa el nombre del participante")
            return
        }
        _form.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            when (
                val result = attendanceRepository.recordWalkIn(
                    eventId = eventId,
                    name = current.name,
                    email = current.email,
                    document = current.document,
                    session = session
                )
            ) {
                is AttendanceResult.Success -> _form.value = WalkInFormState(
                    name = current.name,
                    success = result.attendance
                )
                is AttendanceResult.Failure -> _form.value = current.copy(
                    saving = false,
                    error = result.message
                )
            }
        }
    }

    fun reset() {
        _form.value = WalkInFormState()
    }
}
