package hr.attendance.ui

import hr.attendance.viewmodel.AttendanceSummaryUiState

data class AttendanceSummaryScreenModel(
    val monthLabel: String,
    val presentLabel: String,
    val absentLabel: String,
    val leaveLabel: String,
    val isLoading: Boolean,
    val errorMessage: String?,
) {
    companion object {
        fun from(state: AttendanceSummaryUiState): AttendanceSummaryScreenModel {
            return when (state) {
                is AttendanceSummaryUiState.Loading -> AttendanceSummaryScreenModel(
                    monthLabel = "",
                    presentLabel = "0",
                    absentLabel = "0",
                    leaveLabel = "0",
                    isLoading = true,
                    errorMessage = null,
                )

                is AttendanceSummaryUiState.Success -> AttendanceSummaryScreenModel(
                    monthLabel = state.summary.month,
                    presentLabel = state.summary.present.toString(),
                    absentLabel = state.summary.absent.toString(),
                    leaveLabel = state.summary.leave.toString(),
                    isLoading = false,
                    errorMessage = null,
                )

                is AttendanceSummaryUiState.Error -> AttendanceSummaryScreenModel(
                    monthLabel = "",
                    presentLabel = "0",
                    absentLabel = "0",
                    leaveLabel = "0",
                    isLoading = false,
                    errorMessage = state.message,
                )

                AttendanceSummaryUiState.Unauthorized -> AttendanceSummaryScreenModel(
                    monthLabel = "",
                    presentLabel = "0",
                    absentLabel = "0",
                    leaveLabel = "0",
                    isLoading = false,
                    errorMessage = "You are not authorized to view this team's attendance.",
                )
            }
        }
    }
}
