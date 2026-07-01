package com.hr.attendance.presentation.summary

import com.hr.attendance.domain.model.MonthlyAttendanceSummary

sealed interface AttendanceSummaryUiState {
    data object Loading : AttendanceSummaryUiState

    data class Success(
        val summary: MonthlyAttendanceSummary,
    ) : AttendanceSummaryUiState

    data class Error(
        val message: String,
    ) : AttendanceSummaryUiState
}
