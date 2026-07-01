package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.Employee
import java.net.HttpURLConnection
import java.net.URI

interface AttendanceApi {
    fun fetchSummary(
        teamId: String,
        year: Int,
        month: Int,
        authToken: String,
    ): Pair<List<Employee>, List<AttendanceRecord>>
}

class HttpAttendanceApi(
    private val baseUrl: String,
) : AttendanceApi {
    override fun fetchSummary(
        teamId: String,
        year: Int,
        month: Int,
        authToken: String,
    ): Pair<List<Employee>, List<AttendanceRecord>> {
        val uri = URI(
            "$baseUrl/api/v1/attendance/summary" +
                "?team_id=$teamId&year=$year&month=$month",
        )
        val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $authToken")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText().orEmpty()
            }
            require(status in 200..299) { "API error ($status): $body" }
            AttendanceResponseParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }
}

class InMemoryAttendanceApi(
    private val employees: List<Employee>,
    private val records: List<AttendanceRecord>,
) : AttendanceApi {
    override fun fetchSummary(
        teamId: String,
        year: Int,
        month: Int,
        authToken: String,
    ): Pair<List<Employee>, List<AttendanceRecord>> {
        val teamEmployees = employees.filter { it.teamId == teamId }
        val employeeIds = teamEmployees.map { it.id }.toSet()
        val monthRecords = records.filter { record ->
            record.employeeId in employeeIds &&
                record.date.year == year &&
                record.date.monthValue == month
        }
        return teamEmployees to monthRecords
    }
}
