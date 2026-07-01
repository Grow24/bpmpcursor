package com.hr.attendance.data.api

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpAttendanceApi(
    private val baseUrl: String,
) : AttendanceApi {

    override suspend fun getSummary(month: String, teamId: String): AttendanceSummaryResponse {
        val query = "month=${encode(month)}&team_id=${encode(teamId)}"
        val url = URL("$baseUrl/api/v1/attendance/summary?$query")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            val statusCode = connection.responseCode
            val body = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText().orEmpty()
            }
            require(statusCode in 200..299) {
                "Attendance summary request failed ($statusCode): $body"
            }
            parseResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(body: String): AttendanceSummaryResponse {
        val json = JSONObject(body)
        val employeesJson = json.optJSONArray("employees") ?: JSONArray()
        val employees = buildList {
            for (index in 0 until employeesJson.length()) {
                add(parseEmployee(employeesJson.getJSONObject(index)))
            }
        }
        return AttendanceSummaryResponse(
            month = json.getString("month"),
            teamId = json.getString("teamId"),
            employees = employees,
        )
    }

    private fun parseEmployee(json: JSONObject): EmployeeDto {
        val recordsJson = json.optJSONArray("records") ?: JSONArray()
        val records = buildList {
            for (index in 0 until recordsJson.length()) {
                val record = recordsJson.getJSONObject(index)
                add(
                    AttendanceRecordDto(
                        date = record.getString("date"),
                        status = record.getString("status"),
                    ),
                )
            }
        }
        return EmployeeDto(
            id = json.getLong("id"),
            name = json.getString("name"),
            teamId = json.getString("teamId"),
            records = records,
        )
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
