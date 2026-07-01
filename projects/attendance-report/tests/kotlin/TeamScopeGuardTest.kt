package com.hr.attendance.tests

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.security.JwtAuth
import com.hr.attendance.security.TeamScopeGuard
import com.hr.attendance.security.UnauthorizedTeamAccessException
import java.util.Base64

fun main() {
    testJwtExtractsTeamId()
    testTeamScopeGuardFiltersRecords()
    testTeamScopeGuardRejectsMismatchedTeam()
    println("All TeamScopeGuard tests passed.")
}

private fun testJwtExtractsTeamId() {
    val payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("""{"team_id":"team-42","sub":"u1"}""".toByteArray())
    val token = "header.$payload.sig"
    val teamId = JwtAuth.extractTeamId(token)
    check(teamId == "team-42") { "Expected team-42, got $teamId" }
}

private fun testTeamScopeGuardFiltersRecords() {
    val guard = TeamScopeGuard()
    val records = listOf(
        record("1", "team-a"),
        record("2", "team-b"),
        record("3", "team-a"),
    )
    val filtered = guard.filterByTeam(records, "team-a")
    check(filtered.size == 2) { "Expected 2 team-a records, got ${filtered.size}" }
}

private fun testTeamScopeGuardRejectsMismatchedTeam() {
    val guard = TeamScopeGuard()
    try {
        guard.assertTeamAccess("team-b", "team-a")
        error("Expected UnauthorizedTeamAccessException")
    } catch (_: UnauthorizedTeamAccessException) {
        // expected
    }
}

private fun record(id: String, teamId: String): AttendanceRecord {
    return AttendanceRecord(
        id = id,
        employeeId = "emp-$id",
        employeeName = "Employee $id",
        date = "2025-06-01",
        status = AttendanceStatus.PRESENT,
        teamId = teamId,
    )
}
