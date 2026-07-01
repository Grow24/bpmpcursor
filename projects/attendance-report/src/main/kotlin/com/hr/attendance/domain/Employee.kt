package com.hr.attendance.domain

data class Employee(
    val id: String,
    val name: String,
    val teamId: String,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
)
