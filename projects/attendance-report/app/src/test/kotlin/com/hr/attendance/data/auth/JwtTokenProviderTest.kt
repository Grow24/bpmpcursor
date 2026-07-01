package com.hr.attendance.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtTokenProviderTest {

    @Test
    fun teamId_returnsValueFromJwtPayload() {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"team_id":"team-alpha","role":"manager"}""".toByteArray())
        val token = "header.$payload.signature"

        val provider = JwtTokenProviderImpl(token)

        assertEquals("team-alpha", provider.teamId())
    }

    @Test
    fun teamId_returnsNullWhenTokenMissing() {
        val provider = JwtTokenProviderImpl(null)

        assertNull(provider.teamId())
    }
}
