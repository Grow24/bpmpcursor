package com.hr.attendance.domain.model

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LEAVE,
}

data class AttendanceRecord(
    val date: String,
    val status: AttendanceStatus,
)

data class Employee(
    val id: Long,
    val name: String,
    val teamId: String,
    val records: List<AttendanceRecord> = emptyList(),
)

data class StatusGroup(
    val status: AttendanceStatus,
    val count: Int,
    val records: List<AttendanceRecord>,
)

data class MonthlyAttendanceSummary(
    val month: String,
    val teamId: String,
    val employees: List<Employee>,
    val groupedByStatus: List<StatusGroup>,
)
