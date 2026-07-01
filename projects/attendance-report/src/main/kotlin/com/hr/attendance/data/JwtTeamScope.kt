package com.hr.attendance.data

import java.util.Base64

/**
 * Extracts [team_id] from a JWT and enforces manager team scope.
 * Managers may only access attendance for their own team.
 */
object JwtTeamScope {

    class UnauthorizedTeamAccessException(
        message: String = "Manager cannot access another team's attendance data",
    ) : SecurityException(message)

    fun teamIdFromToken(bearerToken: String): String? {
        val token = bearerToken.removePrefix("Bearer ").trim()
        val parts = token.split(".")
        if (parts.size < 2) return null

        val payloadJson = decodeBase64Url(parts[1]) ?: return null
        return extractJsonString(payloadJson, "team_id")
    }

    fun requireTeamAccess(tokenTeamId: String?, requestedTeamId: String) {
        if (tokenTeamId.isNullOrBlank() || tokenTeamId != requestedTeamId) {
            throw UnauthorizedTeamAccessException()
        }
    }

    private fun decodeBase64Url(segment: String): String? {
        return try {
            val padded = segment + "=".repeat((4 - segment.length % 4) % 4)
            String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }
}
