package com.hr.attendance.presentation.summary

import com.hr.attendance.domain.model.EmployeeAttendanceSummary

/**
 * Mobile screen composable contract for the monthly team attendance report.
 * Renders present / absent / leave counts per employee and team totals.
 */
interface AttendanceSummaryScreen {
    fun render(state: AttendanceSummaryUiState)
}

class AttendanceSummaryScreenImpl : AttendanceSummaryScreen {

    private val renderedLines = mutableListOf<String>()

    val lastRendered: List<String>
        get() = renderedLines.toList()

    override fun render(state: AttendanceSummaryUiState) {
        renderedLines.clear()
        when (state) {
            is AttendanceSummaryUiState.Loading -> {
                renderedLines += "Loading team attendance..."
            }

            is AttendanceSummaryUiState.Error -> {
                renderedLines += "Error: ${state.message}"
            }

            is AttendanceSummaryUiState.Success -> {
                val summary = state.summary
                renderedLines += "Team ${summary.teamId} — ${summary.month}/${summary.year}"
                renderedLines += "Present: ${state.statusTotals.present}"
                renderedLines += "Absent: ${state.statusTotals.absent}"
                renderedLines += "Leave: ${state.statusTotals.leave}"
                summary.employees.forEach { employeeSummary ->
                    renderedLines += formatEmployeeRow(employeeSummary)
                }
            }
        }
    }

    private fun formatEmployeeRow(summary: EmployeeAttendanceSummary): String =
        "${summary.employee.name}: " +
            "P=${summary.presentDays} A=${summary.absentDays} L=${summary.leaveDays}"
}
