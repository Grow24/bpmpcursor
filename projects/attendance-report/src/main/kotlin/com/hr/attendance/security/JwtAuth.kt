package com.hr.attendance.security

import java.util.Base64

data class JwtClaims(
    val teamId: String,
    val userId: String,
    val role: String,
)

object JwtAuth {
    fun parseToken(token: String): JwtClaims? {
        val payload = decodePayload(token) ?: return null
        val teamId = extractJsonString(payload, "team_id") ?: return null
        val userId = extractJsonString(payload, "sub") ?: extractJsonString(payload, "user_id") ?: ""
        val role = extractJsonString(payload, "role") ?: "manager"
        return JwtClaims(teamId = teamId, userId = userId, role = role)
    }

    fun extractTeamId(token: String): String? = parseToken(token)?.teamId

    private fun decodePayload(token: String): String? {
        val normalized = token.removePrefix("Bearer ").trim()
        val parts = normalized.split(".")
        if (parts.size < 2) return null
        return try {
            val decoded = Base64.getUrlDecoder().decode(parts[1])
            decoded.toString(Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun extractJsonString(payload: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return pattern.find(payload)?.groupValues?.get(1)
    }
}
