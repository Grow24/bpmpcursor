package com.hr.attendance.data.remote.dto

import com.hr.attendance.domain.model.AttendanceStatus

data class AttendanceRecordDto(
    val employeeId: String,
    val date: String,
    val status: String,
)

data class EmployeeDto(
    val id: String,
    val name: String,
    val teamId: String,
)

data class AttendanceSummaryResponse(
    val teamId: String,
    val month: Int,
    val year: Int,
    val employees: List<EmployeeDto>,
    val records: List<AttendanceRecordDto>,
) {
    fun parseStatus(raw: String): AttendanceStatus =
        when (raw.uppercase()) {
            "PRESENT" -> AttendanceStatus.PRESENT
            "ABSENT" -> AttendanceStatus.ABSENT
            "LEAVE" -> AttendanceStatus.LEAVE
            else -> throw IllegalArgumentException("Unknown attendance status: $raw")
        }
}
