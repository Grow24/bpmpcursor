package hr.attendance.model

import java.time.LocalDate

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val teamId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
)
