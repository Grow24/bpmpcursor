package com.hr.attendance.presentation

import com.hr.attendance.domain.EmployeeAttendanceSummary

sealed interface AttendanceReportUiState {
    data object Loading : AttendanceReportUiState

    data class Success(
        val monthLabel: String,
        val teamId: String,
        val employees: List<EmployeeAttendanceSummary>,
        val totals: StatusTotals,
    ) : AttendanceReportUiState

    data class Error(val message: String) : AttendanceReportUiState
}

data class StatusTotals(
    val present: Int,
    val absent: Int,
    val leave: Int,
)

fun List<EmployeeAttendanceSummary>.toStatusTotals(): StatusTotals {
    return StatusTotals(
        present = sumOf { it.present },
        absent = sumOf { it.absent },
        leave = sumOf { it.leave },
    )
}
