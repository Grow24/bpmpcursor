package com.hr.attendance.auth

class TeamAuthGuard {
    fun requireManagerTeamId(jwtToken: String): String {
        val claims = JwtDecoder.decode(jwtToken)
        require(claims.role.equals("manager", ignoreCase = true)) {
            "Only managers can view team attendance"
        }
        require(claims.teamId.isNotBlank()) { "Missing team_id in JWT token" }
        return claims.teamId
    }

    fun assertTeamScope(jwtToken: String, requestedTeamId: String) {
        val tokenTeamId = requireManagerTeamId(jwtToken)
        require(tokenTeamId == requestedTeamId) {
            "Access denied: manager can only view their own team (team_id=$tokenTeamId)"
        }
    }
}
