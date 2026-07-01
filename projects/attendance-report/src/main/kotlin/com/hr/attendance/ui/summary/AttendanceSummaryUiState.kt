package com.hr.attendance.ui.summary

import com.hr.attendance.data.model.AttendanceSummaryResponse

sealed interface AttendanceSummaryUiState {
    data object Loading : AttendanceSummaryUiState

    data class Success(val summary: AttendanceSummaryResponse) : AttendanceSummaryUiState

    data class Error(val message: String) : AttendanceSummaryUiState
}
