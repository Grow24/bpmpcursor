package com.hr.attendance.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class JwtTeamAuthTest {

    private val auth = JwtTeamAuth()

    @Test
    fun authenticate_extractsTeamIdFromJwt() {
        val token = jwt(teamId = "team-alpha", subject = "mgr-1")

        val session = auth.authenticate(token)

        assertEquals("team-alpha", session.teamId)
        assertEquals("mgr-1", session.managerId)
    }

    @Test
    fun authenticate_rejectsMissingTeamId() {
        val token = jwt(teamId = "", subject = "mgr-1")

        assertThrows(TeamAccessDeniedException::class.java) {
            auth.authenticate(token)
        }
    }

    private fun jwt(teamId: String, subject: String): String {
        val header = base64Url("""{"alg":"none","typ":"JWT"}""")
        val payload = base64Url("""{"sub":"$subject","team_id":"$teamId"}""")
        return "$header.$payload.signature"
    }

    private fun base64Url(value: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    }
}
