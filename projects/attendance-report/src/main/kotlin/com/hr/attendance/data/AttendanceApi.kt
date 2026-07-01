package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.Employee

/**
 * Contract for GET /api/v1/attendance/summary
 */
interface AttendanceApi {
    suspend fun fetchSummary(month: String, teamId: String): AttendanceSummaryResponse
}

data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val employees: List<Employee>,
    val records: List<AttendanceRecord>,
)
