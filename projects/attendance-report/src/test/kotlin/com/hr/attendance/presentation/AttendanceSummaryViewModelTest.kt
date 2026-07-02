package com.hr.attendance.presentation

import com.hr.attendance.data.AttendanceApiClient
import com.hr.attendance.data.AttendanceRecordDto
import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.data.AttendanceSummaryResponse
import com.hr.attendance.domain.AttendanceStatus
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AttendanceSummaryViewModelTest {
    @Test
    fun viewModelGroupsByStatus() {
        val repository = AttendanceRepository(FakeApiClient())
        val viewModel = AttendanceSummaryViewModel(repository)

        viewModel.loadMonthlySummary(buildJwt(TEAM_A), "2025-06")

        val state = viewModel.uiState
        assertEquals(false, state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.presentCount)
        assertEquals(1, state.absentCount)
        assertEquals(1, state.leaveCount)
        assertEquals(1, state.recordsByStatus[AttendanceStatus.PRESENT]?.size)
    }

    @Test
    fun teamScopeBlocksCrossTeamAccess() {
        val repository = AttendanceRepository(CrossTeamApiClient())
        val viewModel = AttendanceSummaryViewModel(repository)

        viewModel.loadMonthlySummary(buildJwt(TEAM_A), "2025-06")

        val state = viewModel.uiState
        assertNotNull(state.error)
        assertEquals(0, state.presentCount)
    }

    private class FakeApiClient : AttendanceApiClient {
        override fun fetchSummary(
            month: String,
            teamId: String,
            authToken: String,
        ): AttendanceSummaryResponse =
            AttendanceSummaryResponse(
                month = month,
                teamId = teamId,
                records = listOf(
                    AttendanceRecordDto("1", "e1", "Alice", "$month-01", "PRESENT", teamId),
                    AttendanceRecordDto("2", "e2", "Bob", "$month-02", "ABSENT", teamId),
                    AttendanceRecordDto("3", "e3", "Carol", "$month-03", "LEAVE", teamId),
                ),
            )
    }

    private class CrossTeamApiClient : AttendanceApiClient {
        override fun fetchSummary(
            month: String,
            teamId: String,
            authToken: String,
        ): AttendanceSummaryResponse =
            AttendanceSummaryResponse(
                month = month,
                teamId = TEAM_B,
                records = listOf(
                    AttendanceRecordDto("9", "e9", "Eve", "$month-01", "PRESENT", TEAM_B),
                ),
            )
    }

    private companion object {
        const val TEAM_A = "team-alpha"
        const val TEAM_B = "team-beta"

        fun buildJwt(teamId: String): String {
            val header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
            val payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"team_id":"$teamId","sub":"mgr-1","role":"manager"}""".toByteArray())
            return "$header.$payload.signature"
        }
    }
}
