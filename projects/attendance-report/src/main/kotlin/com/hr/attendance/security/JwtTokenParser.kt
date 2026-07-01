package com.hr.attendance.security

import android.util.Base64
import org.json.JSONObject

/**
 * Reads claims from a JWT bearer token without verifying the signature.
 * Signature verification is performed server-side; the mobile client only
 * extracts the manager's team scope for local authorization checks.
 */
class JwtTokenParser {

    fun parseClaims(token: String): JwtClaims {
        val payload = token.substringAfter("Bearer ", token).trim()
        val parts = payload.split(".")
        require(parts.size >= 2) { "Invalid JWT format" }

        val decoded = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8,
        )
        val json = JSONObject(decoded)

        return JwtClaims(
            subject = json.optString("sub"),
            teamId = json.optString("team_id"),
            role = json.optString("role"),
        )
    }
}

data class JwtClaims(
    val subject: String,
    val teamId: String,
    val role: String,
)
