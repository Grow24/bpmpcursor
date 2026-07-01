package com.hr.attendance.domain

import java.time.LocalDate

/**
 * A single day's attendance for an employee.
 * Employee 1..* AttendanceRecord (REQ-2025-021).
 */
data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
)
