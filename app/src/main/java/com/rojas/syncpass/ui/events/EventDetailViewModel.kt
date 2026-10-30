package com.rojas.syncpass.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.local.AppDatabase
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EnrollmentEntity
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.SyncOutcome
import com.rojas.syncpass.data.repository.SyncRepository
import com.rojas.syncpass.data.repository.canBeSurveyedBy
import com.rojas.syncpass.data.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val eventId: Long,
    private val session: Session,
    private val eventRepository: EventRepository,
    private val syncRepository: SyncRepository,
    database: AppDatabase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val event: StateFlow<EventEntity?> = eventRepository.event(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val attendance: StateFlow<List<AttendanceEntity>> = database.attendanceDao().observeByEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enrollments: StateFlow<List<EnrollmentEntity>> = eventRepository.enrollmentsForEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Survey content never reaches a monitor, not even content collected by that monitor.
    val surveys: StateFlow<List<SurveyEntity>> = if (session.isCoordinator) {
        database.surveyDao().observeByEvent(eventId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow<List<SurveyEntity>>(emptyList()).asStateFlow()
    }

    val mySurvey: StateFlow<SurveyEntity?> =
        database.surveyDao().observeForUser(eventId, session.userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val surveyedAttendanceIds: StateFlow<List<String>> =
        database.surveyDao().observeSurveyedAttendanceIds(eventId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val eligibleAttendance: StateFlow<List<AttendanceEntity>> =
        combine(attendance, surveyedAttendanceIds, mySurvey) { rows, surveyed, mine ->
            rows.filter {
                it.canBeSurveyedBy(session) &&
                    it.clientId !in surveyed &&
                    !(mine != null && it.userId == session.userId)
            }.sortedByDescending { it.registeredAt }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = networkMonitor.online

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

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
                val result = eventRepository.refreshEvent(eventId)
                if (result.isFailure && notify) {
                    _message.value = result.exceptionOrNull()?.message ?: "No se pudo actualizar el evento"
                }
                when (val outcome = syncRepository.syncNow()) {
                    is SyncOutcome.Success -> if (notify) {
                        _message.value = "Datos actualizados. ${outcome.downloaded} registros descargados."
                    }
                    is SyncOutcome.Error -> if (notify) {
                        _message.value = "No se pudo sincronizar: ${outcome.message}"
                    }
                    else -> Unit
                }
            } else if (notify) {
                _message.value = "Sin conexión. Se muestran los datos guardados."
            }
            _refreshing.value = false
        }
    }

    fun enroll() {
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            eventRepository.enroll(eventId, session.userId)
                .onSuccess { _message.value = "Inscripción confirmada. ¡Te esperamos!" }
                .onFailure { _message.value = it.message }
            _working.value = false
        }
    }

    fun cancelEnrollment() {
        if (_working.value) return
        viewModelScope.launch {
            _working.value = true
            eventRepository.cancelEnrollment(eventId, session.userId)
                .onSuccess { _message.value = "Inscripción cancelada" }
                .onFailure { _message.value = it.message }
            _working.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
