package com.hr.attendance.tests

import com.hr.attendance.data.AttendanceApiClient
import com.hr.attendance.data.AttendanceRecordDto
import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.data.AttendanceSummaryResponse
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.presentation.AttendanceSummaryViewModel
import com.hr.attendance.security.TeamScopeGuard
import com.hr.attendance.security.UnauthorizedTeamAccessException
import java.util.Base64

private const val TEAM_A = "team-alpha"
private const val TEAM_B = "team-beta"

private fun buildJwt(teamId: String): String {
    val header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
    val payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("""{"team_id":"$teamId","sub":"mgr-1","role":"manager"}""".toByteArray())
    return "$header.$payload.signature"
}

private class FakeApiClient(
    private val teamId: String,
) : AttendanceApiClient {
    override fun fetchSummary(month: String, teamId: String, authToken: String): AttendanceSummaryResponse {
        return AttendanceSummaryResponse(
            month = month,
            teamId = teamId,
            records = listOf(
                AttendanceRecordDto("1", "e1", "Alice", "$month-01", "PRESENT", teamId),
                AttendanceRecordDto("2", "e2", "Bob", "$month-02", "ABSENT", teamId),
                AttendanceRecordDto("3", "e3", "Carol", "$month-03", "LEAVE", teamId),
            ),
        )
    }
}

private class CrossTeamApiClient : AttendanceApiClient {
    override fun fetchSummary(month: String, teamId: String, authToken: String): AttendanceSummaryResponse {
        return AttendanceSummaryResponse(
            month = month,
            teamId = TEAM_B,
            records = listOf(
                AttendanceRecordDto("9", "e9", "Eve", "$month-01", "PRESENT", TEAM_B),
            ),
        )
    }
}

fun main() {
    testViewModelGroupsByStatus()
    testTeamScopeBlocksCrossTeamAccess()
    println("All AttendanceSummaryViewModel tests passed.")
}

private fun testViewModelGroupsByStatus() {
    val repository = AttendanceRepository(FakeApiClient(TEAM_A))
    val viewModel = AttendanceSummaryViewModel(repository)
    val token = buildJwt(TEAM_A)

    viewModel.loadMonthlySummary(token, "2025-06")

    val state = viewModel.uiState
    check(!state.isLoading) { "Expected loading to finish" }
    check(state.error == null) { "Unexpected error: ${state.error}" }
    check(state.presentCount == 1) { "Expected 1 present, got ${state.presentCount}" }
    check(state.absentCount == 1) { "Expected 1 absent, got ${state.absentCount}" }
    check(state.leaveCount == 1) { "Expected 1 leave, got ${state.leaveCount}" }
    check(state.recordsByStatus[AttendanceStatus.PRESENT]?.size == 1)
}

private fun testTeamScopeBlocksCrossTeamAccess() {
    val repository = AttendanceRepository(CrossTeamApiClient())
    val viewModel = AttendanceSummaryViewModel(repository)
    val token = buildJwt(TEAM_A)

    viewModel.loadMonthlySummary(token, "2025-06")

    val state = viewModel.uiState
    check(state.error != null) { "Expected unauthorized error for cross-team access" }
    check(state.presentCount == 0) { "Counts should remain zero on error" }
}
