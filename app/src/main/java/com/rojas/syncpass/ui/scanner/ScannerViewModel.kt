package com.rojas.syncpass.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.data.repository.AttendanceRepository
import com.rojas.syncpass.data.repository.AttendanceResult
import com.rojas.syncpass.data.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val busy: Boolean = false,
    val success: AttendanceResult.Success? = null,
    val error: String? = null
)

class ScannerViewModel(
    private val repository: AttendanceRepository,
    private val session: Session
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onCode(rawCode: String) {
        val current = _uiState.value
        if (current.busy || current.success != null || rawCode.isBlank()) return
        _uiState.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            when (val result = repository.recordQr(rawCode, session)) {
                is AttendanceResult.Success -> _uiState.value = ScannerUiState(success = result)
                is AttendanceResult.Failure -> _uiState.value = ScannerUiState(error = result.message)
            }
        }
    }

    fun reset() {
        _uiState.value = ScannerUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
