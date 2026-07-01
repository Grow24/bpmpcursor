package com.hr.attendance.ui

import com.hr.attendance.viewmodel.AttendanceReportUiState

object AttendanceReportScreen {
  fun render(state: AttendanceReportUiState): String {
    return when (state) {
      AttendanceReportUiState.Loading -> "Loading monthly attendance…"

      is AttendanceReportUiState.Error ->
        "Unable to load report: ${state.message}"

      is AttendanceReportUiState.Success -> {
        val summary = state.summary
        buildString {
          appendLine("Monthly Attendance — ${summary.month}/${summary.year}")
          appendLine("Team: ${summary.teamId}")
          appendLine(
            "Present: ${summary.totalPresent}  |  " +
              "Absent: ${summary.totalAbsent}  |  " +
              "Leave: ${summary.totalLeave}",
          )
          appendLine()
          summary.employees.forEach { employeeSummary ->
            appendLine(
              "${employeeSummary.employee.name}: " +
                "P=${employeeSummary.presentDays} " +
                "A=${employeeSummary.absentDays} " +
                "L=${employeeSummary.leaveDays}",
            )
          }
        }.trimEnd()
      }
    }
  }
}
