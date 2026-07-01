package com.hr.attendance.domain

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LEAVE,
}

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val date: String,
    val status: AttendanceStatus,
)

data class Employee(
    val id: String,
    val name: String,
    val teamId: String,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
)

data class AttendanceSummary(
    val month: String,
    val teamId: String,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val employees: List<Employee>,
)
