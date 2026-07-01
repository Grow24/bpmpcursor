package com.hr.attendance.viewmodel

import com.hr.attendance.data.AttendanceRepository

class AttendanceReportViewModel(
    private val repository: AttendanceRepository,
) {
    private var _uiState: AttendanceReportUiState = AttendanceReportUiState.Loading
    val uiState: AttendanceReportUiState
        get() = _uiState

    var onStateChanged: ((AttendanceReportUiState) -> Unit)? = null

    fun loadMonthlyReport(
        jwtToken: String,
        year: Int,
        month: Int,
    ) {
        updateState(AttendanceReportUiState.Loading)

        try {
            val summary = repository.getMonthlySummary(jwtToken, year, month)
            updateState(AttendanceReportUiState.Success(summary))
        } catch (error: SecurityException) {
            updateState(AttendanceReportUiState.Error(error.message ?: "Access denied"))
        } catch (error: Exception) {
            updateState(AttendanceReportUiState.Error(error.message ?: "Failed to load attendance"))
        }
    }

    private fun updateState(state: AttendanceReportUiState) {
        _uiState = state
        onStateChanged?.invoke(state)
    }
}
