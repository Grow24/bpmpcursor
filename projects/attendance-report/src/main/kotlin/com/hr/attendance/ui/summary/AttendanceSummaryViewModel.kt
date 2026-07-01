package com.hr.attendance.ui.summary

import com.hr.attendance.data.auth.JwtClaims
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the manager's monthly attendance screen.
 *
 * Pseudocode: auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
 */
class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {

    private val _uiState = MutableStateFlow<AttendanceSummaryUiState>(AttendanceSummaryUiState.Loading)
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    fun loadSummary(claims: JwtClaims, month: String, bearerToken: String) {
        scope.launch {
            _uiState.value = AttendanceSummaryUiState.Loading
            _uiState.value = try {
                val summary = repository.getMonthlySummary(
                    claims = claims,
                    month = month,
                    bearerToken = bearerToken,
                )
                AttendanceSummaryUiState.Success(summary)
            } catch (ex: UnauthorizedTeamAccessException) {
                AttendanceSummaryUiState.Error(ex.message ?: "Access denied")
            } catch (ex: Exception) {
                AttendanceSummaryUiState.Error(ex.message ?: "Failed to load attendance summary")
            }
        }
    }
}
