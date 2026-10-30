package com.rojas.syncpass.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.data.repository.ImpactMetrics
import com.rojas.syncpass.data.repository.ReportPeriod
import com.rojas.syncpass.data.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(repository: ReportRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.ALL)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    val metrics: StateFlow<ImpactMetrics> = _period
        .flatMapLatest { repository.observe(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ImpactMetrics(
                totalProjects = 0,
                totalEvents = 0,
                totalEnrollments = 0,
                totalAttendances = 0,
                uniqueParticipants = 0,
                attendanceRate = 0.0,
                averageSatisfaction = 0.0,
                recommendRate = 0.0,
                events = emptyList()
            )
        )

    fun setPeriod(period: ReportPeriod) {
        _period.value = period
    }
}
