package com.hr.attendance.domain.model

data class EmployeeAttendanceSummary(
    val employee: Employee,
    val presentDays: Int,
    val absentDays: Int,
    val leaveDays: Int,
)

data class TeamAttendanceSummary(
    val teamId: String,
    val month: Int,
    val year: Int,
    val employees: List<EmployeeAttendanceSummary>,
) {
    val totalPresent: Int
        get() = employees.sumOf { it.presentDays }

    val totalAbsent: Int
        get() = employees.sumOf { it.absentDays }

    val totalLeave: Int
        get() = employees.sumOf { it.leaveDays }
}
