package com.hr.attendance.data

import com.hr.attendance.model.AttendanceStatus
import com.hr.attendance.model.AttendanceSummaryResponse
import com.hr.attendance.model.Employee
import com.hr.attendance.model.EmployeeAttendanceSummary
import java.time.LocalDate
import java.time.YearMonth

/**
 * Client for GET /api/v1/attendance/summary.
 */
interface AttendanceApiService {
    suspend fun fetchSummary(month: YearMonth, authHeader: String): AttendanceSummaryResponse
}

/**
 * Retrofit-style implementation; uses an injectable HTTP client for testing.
 */
class AttendanceApiServiceImpl(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) : AttendanceApiService {

    override suspend fun fetchSummary(
        month: YearMonth,
        authHeader: String,
    ): AttendanceSummaryResponse {
        val url = "$baseUrl/api/v1/attendance/summary?month=${month}"
        val json = httpClient.get(url, authHeader)
        return AttendanceResponseMapper.fromJson(json)
    }
}

interface HttpClient {
    suspend fun get(url: String, authHeader: String): String
}

/**
 * Maps API JSON into domain objects. Keeps parsing out of the ViewModel.
 */
object AttendanceResponseMapper {

    fun fromJson(json: String): AttendanceSummaryResponse {
        val root = org.json.JSONObject(json)
        val employees = mutableListOf<EmployeeAttendanceSummary>()
        val employeeArray = root.getJSONArray("employees")

        for (i in 0 until employeeArray.length()) {
            val item = employeeArray.getJSONObject(i)
            val employeeJson = item.getJSONObject("employee")
            val employee = Employee(
                id = employeeJson.getString("id"),
                name = employeeJson.getString("name"),
                teamId = employeeJson.getString("teamId"),
            )
            val records = mutableListOf<com.hr.attendance.model.AttendanceRecord>()
            val recordsArray = item.getJSONArray("records")
            for (j in 0 until recordsArray.length()) {
                val recordJson = recordsArray.getJSONObject(j)
                records.add(
                    com.hr.attendance.model.AttendanceRecord(
                        employeeId = employee.id,
                        date = LocalDate.parse(recordJson.getString("date")),
                        status = AttendanceStatus.valueOf(recordJson.getString("status")),
                    ),
                )
            }
            employees.add(EmployeeAttendanceSummary(employee, records))
        }

        return AttendanceSummaryResponse(
            month = root.getString("month"),
            teamId = root.getString("teamId"),
            employees = employees,
        )
    }
}
