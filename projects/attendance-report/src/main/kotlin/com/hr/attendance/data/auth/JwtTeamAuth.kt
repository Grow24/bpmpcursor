package com.hr.attendance.data.auth

/**
 * Reads the manager's team scope from the JWT payload.
 * A manager can only see their own team's data (team_id claim).
 */
interface JwtTeamAuth {
    fun teamIdFromToken(token: String): String?
}

class JwtTeamAuthImpl : JwtTeamAuth {
    override fun teamIdFromToken(token: String): String? {
        val payload = decodePayload(token) ?: return null
        return payload["team_id"]?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun decodePayload(token: String): Map<String, String>? {
        val parts = token.removePrefix("Bearer ").trim().split(".")
        if (parts.size < 2) return null

        val json = base64UrlDecode(parts[1]) ?: return null
        return parseSimpleJson(json)
    }

    private fun base64UrlDecode(segment: String): String? {
        val padded = segment.padEnd(segment.length + (4 - segment.length % 4) % 4, '=')
            .replace('-', '+')
            .replace('_', '/')
        return try {
            String(java.util.Base64.getDecoder().decode(padded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Minimal JSON parser for flat string claims (team_id, sub, role).
     */
    private fun parseSimpleJson(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = Regex(""""([^"]+)"\s*:\s*"([^"]*)"""")
        pattern.findAll(json).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }
}

class UnauthorizedTeamAccessException(
    message: String = "Manager may only view their own team's attendance",
) : SecurityException(message)
