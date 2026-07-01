package com.hr.attendance.presentation

/**
 * Mobile screen that renders the monthly attendance summary for a manager's team.
 * Pseudocode: auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
 */
object AttendanceReportScreen {
    fun render(state: AttendanceSummaryUiState): String {
        if (state.isLoading) return "Loading attendance for ${state.month}..."
        if (state.error != null) return "Error: ${state.error}"

        return buildString {
            appendLine("Team Attendance — ${state.month}")
            appendLine("Present: ${state.presentCount}")
            appendLine("Absent: ${state.absentCount}")
            appendLine("Leave: ${state.leaveCount}")
        }
    }
}
