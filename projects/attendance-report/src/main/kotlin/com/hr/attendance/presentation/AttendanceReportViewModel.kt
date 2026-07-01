package com.hr.attendance.presentation

import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.data.JwtTeamScope
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceReportViewModel(
    private val repository: AttendanceRepository,
    private val bearerToken: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val _uiState = MutableStateFlow<AttendanceReportUiState>(AttendanceReportUiState.Loading)
    val uiState: StateFlow<AttendanceReportUiState> = _uiState.asStateFlow()

    fun loadMonth(month: YearMonth = YearMonth.now()) {
        CoroutineScope(dispatcher).launch {
            _uiState.value = AttendanceReportUiState.Loading
            _uiState.value = try {
                val summary = repository.getTeamMonthlySummary(bearerToken, month)
                AttendanceReportUiState.Success(
                    monthLabel = formatMonth(month),
                    teamId = summary.teamId,
                    employees = summary.employees,
                    totals = summary.employees.toStatusTotals(),
                )
            } catch (ex: JwtTeamScope.UnauthorizedTeamAccessException) {
                AttendanceReportUiState.Error("You can only view your own team's attendance.")
            } catch (ex: Exception) {
                AttendanceReportUiState.Error(ex.message ?: "Unable to load attendance summary.")
            }
        }
    }

    private fun formatMonth(month: YearMonth): String {
        val name = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$name ${month.year}"
    }
}
