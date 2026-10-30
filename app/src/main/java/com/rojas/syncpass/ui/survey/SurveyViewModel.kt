package com.rojas.syncpass.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.data.local.AttendanceDao
import com.rojas.syncpass.data.local.AttendanceEntity
import com.rojas.syncpass.data.local.EventEntity
import com.rojas.syncpass.data.local.SurveyEntity
import com.rojas.syncpass.data.repository.EventRepository
import com.rojas.syncpass.data.repository.SurveyRepository
import com.rojas.syncpass.data.repository.SurveyResult
import com.rojas.syncpass.data.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SurveyFormState(
    val general: Int = 5,
    val organization: Int = 5,
    val impact: Int = 5,
    val comment: String = "",
    val wouldRecommend: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val submitted: SurveyEntity? = null
)

class SurveyViewModel(
    private val eventId: Long,
    private val session: Session,
    private val attendanceClientId: String?,
    eventRepository: EventRepository,
    private val surveyRepository: SurveyRepository,
    attendanceDao: AttendanceDao
) : ViewModel() {

    val event: StateFlow<EventEntity?> = eventRepository.event(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val assisted: Boolean = attendanceClientId != null

    val attendance: StateFlow<AttendanceEntity?> = if (attendanceClientId != null) {
        attendanceDao.observeByClientId(attendanceClientId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    } else {
        MutableStateFlow<AttendanceEntity?>(null).asStateFlow()
    }

    private val _form = MutableStateFlow(SurveyFormState())
    val form: StateFlow<SurveyFormState> = _form.asStateFlow()

    fun onGeneralChange(value: Int) {
        _form.value = _form.value.copy(general = value, error = null)
    }

    fun onOrganizationChange(value: Int) {
        _form.value = _form.value.copy(organization = value, error = null)
    }

    fun onImpactChange(value: Int) {
        _form.value = _form.value.copy(impact = value, error = null)
    }

    fun onCommentChange(value: String) {
        if (value.length <= 600) _form.value = _form.value.copy(comment = value)
    }

    fun onRecommendChange(value: Boolean) {
        _form.value = _form.value.copy(wouldRecommend = value)
    }

    fun submit() {
        val current = _form.value
        if (current.saving || current.submitted != null) return
        _form.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            val result = if (attendanceClientId != null) {
                surveyRepository.submitAssisted(
                    eventId = eventId,
                    session = session,
                    attendanceClientId = attendanceClientId,
                    rating = current.general,
                    organizationRating = current.organization,
                    impactRating = current.impact,
                    comment = current.comment,
                    wouldRecommend = current.wouldRecommend
                )
            } else {
                surveyRepository.submit(
                    eventId = eventId,
                    session = session,
                    rating = current.general,
                    organizationRating = current.organization,
                    impactRating = current.impact,
                    comment = current.comment,
                    wouldRecommend = current.wouldRecommend
                )
            }
            when (result) {
                is SurveyResult.Success -> _form.value = current.copy(
                    saving = false,
                    submitted = result.survey
                )
                is SurveyResult.Failure -> _form.value = current.copy(
                    saving = false,
                    error = result.message
                )
            }
        }
    }

    fun reset() {
        _form.value = SurveyFormState()
    }
}
