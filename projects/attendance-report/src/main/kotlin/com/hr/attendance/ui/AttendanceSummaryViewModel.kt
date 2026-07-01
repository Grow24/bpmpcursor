package com.hr.attendance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.model.AttendanceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * MVVM ViewModel: fetchRecords(month) -> groupBy(status) -> expose UI state.
 */
class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
    private val authHeaderProvider: () -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceSummaryUiState>(AttendanceSummaryUiState.Loading)
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary(month: YearMonth = _selectedMonth.value) {
        _selectedMonth.value = month
        _uiState.value = AttendanceSummaryUiState.Loading
        viewModelScope.launch {
            runCatching {
                repository.loadMonthlySummary(month, authHeaderProvider())
            }.onSuccess { summary ->
                val statusTotals = AttendanceStatus.entries.associateWith { status ->
                    summary.groupedByStatus[status]?.size ?: 0
                }
                _uiState.value = AttendanceSummaryUiState.Success(
                    month = summary.month,
                    teamId = summary.teamId,
                    employees = summary.employees,
                    statusTotals = statusTotals,
                )
            }.onFailure { error ->
                _uiState.value = AttendanceSummaryUiState.Error(
                    message = error.message ?: "Failed to load attendance summary",
                )
            }
        }
    }

    fun onMonthChanged(month: YearMonth) {
        loadSummary(month)
    }
}
