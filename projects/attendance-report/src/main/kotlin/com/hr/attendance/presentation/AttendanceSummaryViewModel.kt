package com.hr.attendance.presentation

import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.security.UnauthorizedTeamAccessException

class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
) {
    var uiState: AttendanceSummaryUiState = AttendanceSummaryUiState()
        private set

    fun loadMonthlySummary(authToken: String, month: String) {
        uiState = uiState.copy(isLoading = true, month = month, error = null)
        try {
            val summary = repository.getMonthlySummary(authToken, month)
            uiState = AttendanceSummaryUiState(
                isLoading = false,
                month = summary.month,
                presentCount = summary.presentCount,
                absentCount = summary.absentCount,
                leaveCount = summary.leaveCount,
                recordsByStatus = summary.recordsByStatus,
            )
        } catch (ex: UnauthorizedTeamAccessException) {
            uiState = uiState.copy(isLoading = false, error = ex.message)
        } catch (ex: Exception) {
            uiState = uiState.copy(isLoading = false, error = ex.message ?: "Failed to load attendance.")
        }
    }

    fun groupByStatus(records: List<AttendanceRecord>): Map<AttendanceStatus, List<AttendanceRecord>> {
        return repository.groupByStatus(records)
    }
}
