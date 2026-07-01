package com.hr.attendance.security

/**
 * Enforces that a manager can only access their own team's attendance data.
 * team_id MUST come from the JWT — never from user-supplied request parameters.
 */
class TeamScopeValidator(
    private val jwtTokenParser: JwtTokenParser = JwtTokenParser(),
) {

    fun authorizeManagerAccess(authHeader: String): String {
        val claims = jwtTokenParser.parseClaims(authHeader)
        require(claims.role == MANAGER_ROLE) {
            "Only managers can view team attendance"
        }
        require(claims.teamId.isNotBlank()) {
            "team_id missing from JWT token"
        }
        return claims.teamId
    }

    fun assertTeamScope(authorizedTeamId: String, responseTeamId: String) {
        require(authorizedTeamId == responseTeamId) {
            "Access denied: attendance data belongs to another team"
        }
    }

    companion object {
        const val MANAGER_ROLE = "manager"
    }
}
