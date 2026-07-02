package com.hr.attendance.security

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TeamScopeGuardTest {
    @Test
    fun jwtExtractsTeamId() {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"team_id":"team-42","sub":"u1"}""".toByteArray())
        val token = "header.$payload.sig"
        assertEquals("team-42", JwtAuth.extractTeamId(token))
    }

    @Test
    fun teamScopeGuardFiltersRecords() {
        val guard = TeamScopeGuard()
        val records = listOf(
            record("1", "team-a"),
            record("2", "team-b"),
            record("3", "team-a"),
        )
        assertEquals(2, guard.filterByTeam(records, "team-a").size)
    }

    @Test
    fun teamScopeGuardRejectsMismatchedTeam() {
        val guard = TeamScopeGuard()
        assertFailsWith<UnauthorizedTeamAccessException> {
            guard.assertTeamAccess("team-b", "team-a")
        }
    }

    private fun record(id: String, teamId: String): AttendanceRecord =
        AttendanceRecord(
            id = id,
            employeeId = "emp-$id",
            employeeName = "Employee $id",
            date = "2025-06-01",
            status = AttendanceStatus.PRESENT,
            teamId = teamId,
        )
}
