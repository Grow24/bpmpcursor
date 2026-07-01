package com.hr.attendance.data.model

data class EmployeeAttendanceSummary(
    val employeeId: String,
    val name: String,
    val present: Int,
    val absent: Int,
    val leave: Int,
)

data class AttendanceTotals(
    val present: Int,
    val absent: Int,
    val leave: Int,
)

data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val employees: List<EmployeeAttendanceSummary>,
    val totals: AttendanceTotals,
)
