package com.hr.attendance.data.auth

class UnauthorizedTeamAccessException(
    message: String = "Manager can only access their own team data.",
) : SecurityException(message)

/**
 * Validates that the manager's JWT [teamId] matches the requested team scope.
 * Security rule: a manager can only see their own team's data.
 */
class JwtTeamValidator {

    fun requireTeamAccess(tokenTeamId: String?, requestedTeamId: String) {
        if (tokenTeamId.isNullOrBlank()) {
            throw UnauthorizedTeamAccessException("Missing team_id in JWT token.")
        }
        if (tokenTeamId != requestedTeamId) {
            throw UnauthorizedTeamAccessException(
                "JWT team_id does not match requested team.",
            )
        }
    }

    fun extractTeamId(jwtPayload: Map<String, String>): String? =
        jwtPayload["team_id"]
}
