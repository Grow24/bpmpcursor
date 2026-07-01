package com.hr.attendance.ui.summary

import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.repository.AttendanceRepository
import java.time.YearMonth

/**
 * MVVM ViewModel for the manager's monthly attendance report screen.
 *
 * Flow: auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
 */
class AttendanceSummaryViewModel(
  private val repository: AttendanceRepository,
) {
  private var currentState: AttendanceSummaryUiState = AttendanceSummaryUiState.Loading

  fun uiState(): AttendanceSummaryUiState = currentState

  fun load(authToken: String, month: YearMonth = YearMonth.now()) {
    currentState = AttendanceSummaryUiState.Loading
    currentState = try {
      val summaries = repository.loadMonthlySummary(authToken, month)
      AttendanceSummaryUiState.Success(
        month = month,
        teamSummaries = summaries,
        totals = AttendanceTotals.fromSummaries(summaries),
      )
    } catch (error: UnauthorizedTeamAccessException) {
      AttendanceSummaryUiState.Error(error.message ?: "Unauthorized")
    } catch (error: Exception) {
      AttendanceSummaryUiState.Error(
        error.message ?: "Unable to load attendance summary",
      )
    }
  }
}
