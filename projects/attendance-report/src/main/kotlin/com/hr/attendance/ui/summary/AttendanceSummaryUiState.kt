package com.hr.attendance.ui.summary

import com.hr.attendance.domain.EmployeeAttendanceSummary
import java.time.YearMonth

sealed interface AttendanceSummaryUiState {
  data object Loading : AttendanceSummaryUiState

  data class Success(
    val month: YearMonth,
    val teamSummaries: List<EmployeeAttendanceSummary>,
    val totals: AttendanceTotals,
  ) : AttendanceSummaryUiState

  data class Error(val message: String) : AttendanceSummaryUiState
}

data class AttendanceTotals(
  val presentDays: Int,
  val absentDays: Int,
  val leaveDays: Int,
) {
  companion object {
    fun fromSummaries(summaries: List<EmployeeAttendanceSummary>): AttendanceTotals {
      return AttendanceTotals(
        presentDays = summaries.sumOf { it.presentDays },
        absentDays = summaries.sumOf { it.absentDays },
        leaveDays = summaries.sumOf { it.leaveDays },
      )
    }
  }
}
