package com.hr.attendance.presentation.summary

import com.hr.attendance.domain.model.EmployeeAttendanceSummary
import com.hr.attendance.domain.model.TeamAttendanceSummary

sealed interface AttendanceSummaryUiState {
    data object Loading : AttendanceSummaryUiState

    data class Success(
        val summary: TeamAttendanceSummary,
        val statusTotals: StatusTotals,
    ) : AttendanceSummaryUiState

    data class Error(val message: String) : AttendanceSummaryUiState
}

data class StatusTotals(
    val present: Int,
    val absent: Int,
    val leave: Int,
) {
    companion object {
        fun from(employees: List<EmployeeAttendanceSummary>): StatusTotals =
            StatusTotals(
                present = employees.sumOf { it.presentDays },
                absent = employees.sumOf { it.absentDays },
                leave = employees.sumOf { it.leaveDays },
            )
    }
}
