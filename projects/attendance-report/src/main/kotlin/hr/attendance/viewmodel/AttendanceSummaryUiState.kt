package hr.attendance.viewmodel

import hr.attendance.model.AttendanceSummary

sealed interface AttendanceSummaryUiState {
    data object Loading : AttendanceSummaryUiState

    data class Success(
        val summary: AttendanceSummary,
    ) : AttendanceSummaryUiState

    data class Error(
        val message: String,
    ) : AttendanceSummaryUiState

    data object Unauthorized : AttendanceSummaryUiState
}
