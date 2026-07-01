package com.hr.attendance.data.auth

import org.json.JSONObject
import java.util.Base64

/**
 * Extracts [team_id] from the manager JWT. Managers may only access records
 * scoped to that team (REQ-2025-021 security rule).
 */
class JwtTeamAuth : TeamAuth {

    override fun authenticate(jwtToken: String): ManagerSession {
        val payload = decodePayload(jwtToken)
        val teamId = payload.optString("team_id").ifBlank {
            throw TeamAccessDeniedException("JWT is missing team_id claim.")
        }
        val managerId = payload.optString("sub").ifBlank { "manager" }
        return ManagerSession(managerId = managerId, teamId = teamId)
    }

    private fun decodePayload(jwtToken: String): JSONObject {
        val parts = jwtToken.split(".")
        require(parts.size >= 2) { "Invalid JWT format." }
        val decoded = Base64.getUrlDecoder().decode(parts[1])
        return JSONObject(String(decoded, Charsets.UTF_8))
    }
}
