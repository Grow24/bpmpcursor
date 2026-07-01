package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus

data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val records: List<AttendanceRecordDto>,
)

data class AttendanceRecordDto(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val status: String,
    val teamId: String,
) {
    fun toDomain(): AttendanceRecord {
        val parsedStatus = runCatching {
            AttendanceStatus.valueOf(status.uppercase())
        }.getOrDefault(AttendanceStatus.ABSENT)
        return AttendanceRecord(
            id = id,
            employeeId = employeeId,
            employeeName = employeeName,
            date = date,
            status = parsedStatus,
            teamId = teamId,
        )
    }
}
