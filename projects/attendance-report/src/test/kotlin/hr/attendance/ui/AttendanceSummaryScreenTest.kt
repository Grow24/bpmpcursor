package hr.attendance.ui

import hr.attendance.model.AttendanceSummary
import hr.attendance.viewmodel.AttendanceSummaryUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceSummaryScreenTest {
    @Test
    fun from_mapsSuccessStateToMobileLabels() {
        val screen = AttendanceSummaryScreenModel.from(
            AttendanceSummaryUiState.Success(
                AttendanceSummary(
                    month = "2025-06",
                    teamId = "team-alpha",
                    present = 18,
                    absent = 2,
                    leave = 1,
                ),
            ),
        )

        assertEquals("2025-06", screen.monthLabel)
        assertEquals("18", screen.presentLabel)
        assertEquals("2", screen.absentLabel)
        assertEquals("1", screen.leaveLabel)
        assertTrue(!screen.isLoading)
    }
}
