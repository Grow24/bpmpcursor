package com.hr.attendance.data.auth

class UnauthorizedTeamAccessException(
    message: String = "Manager can only access their own team data",
) : SecurityException(message)

interface JwtTokenProvider {
    fun teamId(): String?
}

class JwtTokenProviderImpl(
    private val token: String?,
) : JwtTokenProvider {

    override fun teamId(): String? {
        if (token.isNullOrBlank()) {
            return null
        }
        val payload = token.split(".")
        if (payload.size < 2) {
            return null
        }
        val decoded = decodeBase64Url(payload[1]) ?: return null
        return TEAM_ID_REGEX.find(decoded)?.groupValues?.get(1)
    }

    private fun decodeBase64Url(segment: String): String? {
        val padded = segment.padEnd(
            segment.length + (4 - segment.length % 4) % 4,
            '=',
        )
        return runCatching {
            String(java.util.Base64.getUrlDecoder().decode(padded))
        }.getOrNull()
    }

    companion object {
        private val TEAM_ID_REGEX = Regex(""""team_id"\s*:\s*"([^"]+)"""")
    }
}
