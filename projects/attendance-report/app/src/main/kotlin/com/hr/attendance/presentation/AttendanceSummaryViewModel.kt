package com.hr.attendance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceSummaryUiState(isLoading = true))
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    fun loadMonthlySummary(month: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, month = month) }
            try {
                val summary = repository.fetchMonthlySummary(month)
                _uiState.value = summary.toUiState()
            } catch (error: UnauthorizedTeamAccessException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load attendance summary",
                    )
                }
            }
        }
    }
}
