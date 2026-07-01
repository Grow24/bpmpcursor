package com.hr.attendance.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hr.attendance.data.auth.TeamAuth
import com.hr.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVVM flow (approved pseudocode):
 * auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
 */
class AttendanceSummaryViewModel(
    private val teamAuth: TeamAuth,
    private val repository: AttendanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceSummaryUiState>(AttendanceSummaryUiState.Loading)
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    fun loadMonthlySummary(jwtToken: String, month: String) {
        viewModelScope.launch {
            _uiState.value = AttendanceSummaryUiState.Loading
            runCatching {
                val session = teamAuth.authenticate(jwtToken)
                repository.fetchMonthlySummary(month = month, teamId = session.teamId)
            }.onSuccess { summary ->
                _uiState.value = AttendanceSummaryUiState.Success(summary)
            }.onFailure { error ->
                _uiState.value = AttendanceSummaryUiState.Error(
                    message = error.message ?: "Unable to load attendance summary.",
                )
            }
        }
    }
}
