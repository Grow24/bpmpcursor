package com.hr.attendance.domain

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val status: AttendanceStatus,
    val teamId: String,
)
