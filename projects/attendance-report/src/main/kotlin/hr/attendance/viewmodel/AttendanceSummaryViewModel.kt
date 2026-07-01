package hr.attendance.viewmodel

import hr.attendance.repository.AttendanceRepository
import hr.attendance.security.JwtClaims
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceSummaryViewModel(
    private val repository: AttendanceRepository,
    private val claims: JwtClaims,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow<AttendanceSummaryUiState>(AttendanceSummaryUiState.Loading)
    val uiState: StateFlow<AttendanceSummaryUiState> = _uiState.asStateFlow()

    fun loadMonthlySummary(month: String) {
        scope.launch {
            _uiState.value = AttendanceSummaryUiState.Loading
            _uiState.value = try {
                val summary = repository.getMonthlySummary(claims, month)
                AttendanceSummaryUiState.Success(summary)
            } catch (error: IllegalArgumentException) {
                if (error.message?.contains("team scope", ignoreCase = true) == true ||
                    error.message?.contains("Only managers", ignoreCase = true) == true
                ) {
                    AttendanceSummaryUiState.Unauthorized
                } else {
                    AttendanceSummaryUiState.Error(error.message ?: "Unable to load attendance summary")
                }
            } catch (error: Exception) {
                AttendanceSummaryUiState.Error(error.message ?: "Unable to load attendance summary")
            }
        }
    }
}
