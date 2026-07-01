package com.hr.attendance.data.model

import java.time.LocalDate

/**
 * Business object for a single day's attendance (REQ-2025-021).
 * Linked to [Employee] via employeeId (Employee 1..* AttendanceRecord).
 */
data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
    val teamId: String,
)
