package com.hr.attendance.model

import java.time.LocalDate

/**
 * Business object for a single day's attendance (REQ-2025-021).
 * Employee 1..* AttendanceRecord.
 */
data class AttendanceRecord(
    val employeeId: String,
    val date: LocalDate,
    val status: AttendanceStatus,
)
