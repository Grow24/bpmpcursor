package com.hr.attendance.data.api

import com.hr.attendance.data.model.AttendanceRecord
import com.hr.attendance.data.model.AttendanceStatus
import java.time.LocalDate

/**
 * Demo implementation for local previews and unit tests.
 * Production builds use [HttpAttendanceApiService] against GET /api/v1/attendance/summary.
 */
class InMemoryAttendanceApiService(
    private val recordsByTeam: Map<String, List<AttendanceRecord>>,
) : AttendanceApiService {

    override suspend fun fetchAttendanceRecords(
        teamId: String,
        month: String,
        bearerToken: String,
    ): List<AttendanceRecord> {
        return recordsByTeam[teamId].orEmpty().filter { record ->
            record.date.toString().startsWith(month)
        }
    }
}

class HttpAttendanceApiService(
    private val baseUrl: String,
    private val httpGet: suspend (url: String, headers: Map<String, String>) -> String,
    private val recordParser: (String) -> List<AttendanceRecord>,
) : AttendanceApiService {

    override suspend fun fetchAttendanceRecords(
        teamId: String,
        month: String,
        bearerToken: String,
    ): List<AttendanceRecord> {
        val url = buildString {
            append(baseUrl.trimEnd('/'))
            append(AttendanceEndpoints.SUMMARY)
            append("?teamId=")
            append(teamId)
            append("&month=")
            append(month)
        }
        val body = httpGet(
            url,
            mapOf("Authorization" to "Bearer $bearerToken"),
        )
        return recordParser(body)
    }
}

fun sampleRecordsForTeam(teamId: String): List<AttendanceRecord> = listOf(
    AttendanceRecord("r1", "e1", LocalDate.parse("2026-06-02"), AttendanceStatus.PRESENT, teamId),
    AttendanceRecord("r2", "e1", LocalDate.parse("2026-06-03"), AttendanceStatus.ABSENT, teamId),
    AttendanceRecord("r3", "e1", LocalDate.parse("2026-06-04"), AttendanceStatus.LEAVE, teamId),
    AttendanceRecord("r4", "e2", LocalDate.parse("2026-06-02"), AttendanceStatus.PRESENT, teamId),
    AttendanceRecord("r5", "e2", LocalDate.parse("2026-06-03"), AttendanceStatus.PRESENT, teamId),
)
