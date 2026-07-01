package com.hr.attendance.domain.model

import java.time.LocalDate

data class AttendanceRecord(
    val employeeId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
)
