package hr.attendance.security

import kotlin.test.Test
import kotlin.test.assertFailsWith

class TeamScopeGuardTest {
    private val guard = TeamScopeGuard()

    @Test
    fun authorize_allowsManagerForOwnTeam() {
        val claims = JwtClaims(subject = "mgr-1", teamId = "team-alpha", role = "manager")
        guard.authorize(claims, "team-alpha")
    }

    @Test
    fun authorize_rejectsDifferentTeam() {
        val claims = JwtClaims(subject = "mgr-1", teamId = "team-alpha", role = "manager")
        assertFailsWith<IllegalArgumentException> {
            guard.authorize(claims, "team-beta")
        }
    }

    @Test
    fun authorize_rejectsNonManagerRole() {
        val claims = JwtClaims(subject = "emp-1", teamId = "team-alpha", role = "employee")
        assertFailsWith<IllegalArgumentException> {
            guard.authorize(claims, "team-alpha")
        }
    }
}
