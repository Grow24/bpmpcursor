package com.hr.attendance.data.api

import java.time.YearMonth

/**
 * HTTP client for GET /api/v1/attendance/summary?year=YYYY&month=MM
 */
class HttpAttendanceSummaryApi(
    private val baseUrl: String,
    private val mapper: AttendanceSummaryResponseMapper = AttendanceSummaryResponseMapper(),
    private val httpGet: (url: String, authToken: String) -> String,
) : AttendanceSummaryApi {
    override fun fetchSummary(
        authToken: String,
        month: YearMonth,
    ): List<AttendanceSummaryItem> {
        val url = buildUrl(month)
        val body = httpGet(url, authToken)
        return mapper.mapEmployees(parseEmployeesJson(body))
    }

    private fun buildUrl(month: YearMonth): String =
        "$baseUrl/api/v1/attendance/summary?year=${month.year}&month=${month.monthValue}"

    private fun parseEmployeesJson(body: String): List<RawEmployee> {
        // Production apps use kotlinx.serialization or Moshi; this keeps the demo dependency-free.
        val employeeBlocks = Regex("""\{[^{}]*"records"\s*:\s*\[[^\]]*][^{}]*}""")
            .findAll(body)
            .map { it.value }
            .toList()
        if (employeeBlocks.isEmpty()) return emptyList()

        return employeeBlocks.map { block ->
            RawEmployee(
                id = extractString(block, "id"),
                name = extractString(block, "name"),
                teamId = extractString(block, "team_id"),
                records = parseRecords(block),
            )
        }
    }

    private fun parseRecords(block: String): List<RawAttendanceRecord> {
        val recordsSection = Regex(""""records"\s*:\s*\[(.*)]""", RegexOption.DOT_MATCHES_ALL)
            .find(block)
            ?.groupValues
            ?.get(1)
            ?: return emptyList()

        return Regex("""\{[^{}]*}""")
            .findAll(recordsSection)
            .map { match ->
                val record = match.value
                RawAttendanceRecord(
                    id = extractString(record, "id"),
                    date = extractString(record, "date"),
                    status = extractString(record, "status"),
                )
            }
            .toList()
    }

    private fun extractString(json: String, key: String): String {
        val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }
}
