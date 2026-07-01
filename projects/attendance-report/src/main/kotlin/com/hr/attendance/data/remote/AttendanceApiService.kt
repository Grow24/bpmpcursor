package com.hr.attendance.data.remote

import com.hr.attendance.data.remote.dto.AttendanceSummaryResponse

/**
 * REST client for GET /api/v1/attendance/summary
 */
interface AttendanceApiService {
    suspend fun getSummary(
        teamId: String,
        month: Int,
        year: Int,
        bearerToken: String,
    ): AttendanceSummaryResponse
}

class AttendanceApiServiceImpl(
    private val baseUrl: String,
    private val httpClient: suspend (String, Map<String, String>) -> String,
) : AttendanceApiService {

    override suspend fun getSummary(
        teamId: String,
        month: Int,
        year: Int,
        bearerToken: String,
    ): AttendanceSummaryResponse {
        val url = buildString {
            append(baseUrl.trimEnd('/'))
            append("/api/v1/attendance/summary")
            append("?teamId=")
            append(teamId)
            append("&month=")
            append(month)
            append("&year=")
            append(year)
        }

        val body = httpClient(
            url,
            mapOf("Authorization" to "Bearer $bearerToken"),
        )
        return AttendanceSummaryResponseParser.parse(body)
    }
}

object AttendanceSummaryResponseParser {
    fun parse(json: String): AttendanceSummaryResponse {
        // Minimal JSON parsing for demo; production would use kotlinx.serialization.
        fun extractInt(key: String): Int =
            Regex(""""$key"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toInt()
                ?: throw IllegalArgumentException("Missing $key in response")

        fun extractString(key: String): String =
            Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Missing $key in response")

        return AttendanceSummaryResponse(
            teamId = extractString("teamId"),
            month = extractInt("month"),
            year = extractInt("year"),
            employees = emptyList(),
            records = emptyList(),
        )
    }
}
