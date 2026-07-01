package com.hr.attendance.ui

import com.hr.attendance.data.EmployeeAttendanceRow
import com.hr.attendance.model.AttendanceStatus
import java.time.YearMonth

/**
 * UI state for the monthly attendance summary screen.
 */
sealed interface AttendanceSummaryUiState {
    data object Loading : AttendanceSummaryUiState

    data class Success(
        val month: YearMonth,
        val teamId: String,
        val employees: List<EmployeeAttendanceRow>,
        val statusTotals: Map<AttendanceStatus, Int>,
    ) : AttendanceSummaryUiState

    data class Error(val message: String) : AttendanceSummaryUiState
}
