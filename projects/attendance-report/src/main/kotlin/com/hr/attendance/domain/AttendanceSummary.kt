package com.hr.attendance.domain

data class EmployeeAttendanceSummary(
    val employeeId: String,
    val employeeName: String,
    val present: Int,
    val absent: Int,
    val leave: Int,
)

data class TeamAttendanceSummary(
    val month: String,
    val teamId: String,
    val employees: List<EmployeeAttendanceSummary>,
)
