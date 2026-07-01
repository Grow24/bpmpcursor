package com.hr.attendance.auth

data class JwtClaims(
    val teamId: String,
    val role: String,
    val subject: String,
)

object JwtDecoder {
    private val teamIdPattern = Regex(""""team_id"\s*:\s*"?([^",}\s]+)"""")
    private val rolePattern = Regex(""""role"\s*:\s*"([^"]+)"""")
    private val subPattern = Regex(""""sub"\s*:\s*"([^"]+)"""")

    fun decode(token: String): JwtClaims {
        val parts = token.split(".")
        require(parts.size >= 2) { "Invalid JWT format" }

        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(padBase64(parts[1])))
        val teamId = teamIdPattern.find(payloadJson)?.groupValues?.get(1)
            ?: throw SecurityException("Missing team_id in JWT token")
        val role = rolePattern.find(payloadJson)?.groupValues?.get(1) ?: "employee"
        val subject = subPattern.find(payloadJson)?.groupValues?.get(1) ?: ""

        return JwtClaims(teamId = teamId, role = role, subject = subject)
    }

    private fun padBase64(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "=".repeat(4 - remainder)
    }
}
