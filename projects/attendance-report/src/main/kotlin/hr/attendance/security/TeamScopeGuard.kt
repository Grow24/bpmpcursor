package hr.attendance.security

class TeamScopeGuard {
    fun authorize(claims: JwtClaims, requestedTeamId: String) {
        require(claims.role == MANAGER_ROLE) {
            "Only managers can view team attendance"
        }
        require(claims.teamId == requestedTeamId) {
            "Access denied: team scope mismatch"
        }
    }

    companion object {
        const val MANAGER_ROLE = "manager"
    }
}
