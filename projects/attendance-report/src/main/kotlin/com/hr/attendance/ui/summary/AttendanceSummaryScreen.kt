package com.hr.attendance.ui.summary

import com.hr.attendance.data.model.AttendanceSummaryResponse
import com.hr.attendance.data.model.EmployeeAttendanceSummary

/**
 * Mobile screen that renders present / absent / leave counts for the manager's team.
 *
 * Wire this composable to [AttendanceSummaryViewModel.uiState] in the hosting Activity.
 */
object AttendanceSummaryScreen {

    fun render(state: AttendanceSummaryUiState): String = when (state) {
        is AttendanceSummaryUiState.Loading -> "Loading monthly attendance..."
        is AttendanceSummaryUiState.Error -> "Error: ${state.message}"
        is AttendanceSummaryUiState.Success -> formatSummary(state.summary)
    }

    private fun formatSummary(summary: AttendanceSummaryResponse): String {
        val header = "Team ${summary.teamId} — ${summary.month}"
        val rows = summary.employees.joinToString("\n") { row(it) }
        val totals = "Totals: present=${summary.totals.present}, absent=${summary.totals.absent}, leave=${summary.totals.leave}"
        return listOf(header, rows, totals).joinToString("\n")
    }

    private fun row(employee: EmployeeAttendanceSummary): String =
        "${employee.name}: present=${employee.present}, absent=${employee.absent}, leave=${employee.leave}"
}
