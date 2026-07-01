package com.hr.attendance.presentation.summary

import com.hr.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel: auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
 */
class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
    private val managerTeamId: String,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow<AttendanceSummaryUiState>(
        AttendanceSummaryUiState.Loading,
    )
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    fun loadMonthlySummary(month: Int, year: Int) {
        scope.launch {
            _uiState.value = AttendanceSummaryUiState.Loading
            try {
                val summary = repository.fetchMonthlySummary(
                    teamId = managerTeamId,
                    month = month,
                    year = year,
                )
                _uiState.value = AttendanceSummaryUiState.Success(
                    summary = summary,
                    statusTotals = StatusTotals.from(summary.employees),
                )
            } catch (e: SecurityException) {
                _uiState.value = AttendanceSummaryUiState.Error(e.message ?: "Access denied.")
            } catch (e: Exception) {
                _uiState.value = AttendanceSummaryUiState.Error(
                    e.message ?: "Failed to load attendance summary.",
                )
            }
        }
    }
}
