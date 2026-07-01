package hr.attendance.api

import hr.attendance.model.AttendanceRecord

data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val records: List<AttendanceRecord>,
)

interface AttendanceApi {
    suspend fun fetchSummary(teamId: String, month: String): AttendanceSummaryResponse
}

const val ATTENDANCE_SUMMARY_PATH = "/api/v1/attendance/summary"
