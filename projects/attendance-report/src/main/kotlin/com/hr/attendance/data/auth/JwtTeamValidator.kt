package com.hr.attendance.data.auth

data class JwtClaims(
    val subject: String,
    val teamId: String,
    val role: String,
)

class UnauthorizedTeamAccessException(
    message: String = "Manager can only access their own team's attendance data.",
) : SecurityException(message)

/**
 * Validates that the authenticated manager's JWT [team_id] matches the requested scope.
 */
class JwtTeamValidator {

    fun requireTeamAccess(claims: JwtClaims, requestedTeamId: String) {
        if (claims.teamId != requestedTeamId) {
            throw UnauthorizedTeamAccessException()
        }
    }

    fun parseTeamIdFromJwtPayload(payloadJson: String): String {
        val key = "\"team_id\""
        val start = payloadJson.indexOf(key)
        if (start < 0) {
            throw IllegalArgumentException("JWT payload missing team_id claim")
        }
        val colon = payloadJson.indexOf(':', start)
        val quoteStart = payloadJson.indexOf('"', colon + 1)
        val quoteEnd = payloadJson.indexOf('"', quoteStart + 1)
        if (quoteStart < 0 || quoteEnd < 0) {
            throw IllegalArgumentException("Invalid team_id claim in JWT")
        }
        return payloadJson.substring(quoteStart + 1, quoteEnd)
    }
}
