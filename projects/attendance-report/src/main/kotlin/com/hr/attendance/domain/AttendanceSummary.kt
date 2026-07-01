package com.hr.attendance.domain

data class AttendanceSummary(
    val month: String,
    val teamId: String,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val recordsByStatus: Map<AttendanceStatus, List<AttendanceRecord>>,
)
