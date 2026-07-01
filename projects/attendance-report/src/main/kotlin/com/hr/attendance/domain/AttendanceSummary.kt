package com.hr.attendance.domain

data class EmployeeMonthlyAttendance(
    val employee: Employee,
    val presentDays: Int,
    val absentDays: Int,
    val leaveDays: Int,
    val records: List<AttendanceRecord>,
)

data class AttendanceSummary(
    val year: Int,
    val month: Int,
    val teamId: String,
    val totalPresent: Int,
    val totalAbsent: Int,
    val totalLeave: Int,
    val employees: List<EmployeeMonthlyAttendance>,
)
