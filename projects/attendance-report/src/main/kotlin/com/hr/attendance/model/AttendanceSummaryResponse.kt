package com.hr.attendance.model

/**
 * Response body for GET /api/v1/attendance/summary.
 */
data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val employees: List<EmployeeAttendanceSummary>,
)

data class EmployeeAttendanceSummary(
    val employee: Employee,
    val records: List<AttendanceRecord>,
)

data class StatusCounts(
    val present: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
) {
    val total: Int get() = present + absent + leave
}
