package com.hr.attendance.ui.summary

import com.hr.attendance.domain.EmployeeAttendanceSummary

/**
 * Mobile screen that renders present / absent / leave counts per team member.
 * Wire to Jetpack Compose or XML from an Android Activity/Fragment.
 */
class AttendanceSummaryScreen(
  private val viewModel: AttendanceSummaryViewModel,
) {
  fun render(state: AttendanceSummaryUiState): String = when (state) {
    AttendanceSummaryUiState.Loading -> "Loading team attendance..."
    is AttendanceSummaryUiState.Error -> "Error: ${state.message}"
    is AttendanceSummaryUiState.Success -> buildSuccessText(state)
  }

  private fun buildSuccessText(state: AttendanceSummaryUiState.Success): String {
    val header = buildString {
      appendLine("Team attendance — ${state.month}")
      appendLine(
        "Totals: Present ${state.totals.presentDays} | " +
          "Absent ${state.totals.absentDays} | Leave ${state.totals.leaveDays}",
      )
      appendLine()
    }
    val rows = state.teamSummaries.joinToString(separator = "\n") { summary ->
      formatEmployeeRow(summary)
    }
    return header + rows
  }

  private fun formatEmployeeRow(summary: EmployeeAttendanceSummary): String =
    "${summary.employee.name}: " +
      "P ${summary.presentDays} / A ${summary.absentDays} / L ${summary.leaveDays}"
}
