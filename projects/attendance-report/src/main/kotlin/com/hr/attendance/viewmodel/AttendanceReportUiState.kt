package com.hr.attendance.viewmodel

import com.hr.attendance.domain.AttendanceSummary

sealed interface AttendanceReportUiState {
    data object Loading : AttendanceReportUiState

    data class Success(
        val summary: AttendanceSummary,
    ) : AttendanceReportUiState

    data class Error(
        val message: String,
    ) : AttendanceReportUiState
}
