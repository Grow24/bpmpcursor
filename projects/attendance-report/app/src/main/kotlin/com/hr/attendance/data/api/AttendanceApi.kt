package com.hr.attendance.data.api

import com.hr.attendance.domain.model.AttendanceRecord
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.Employee

/**
 * REST contract: GET /api/v1/attendance/summary
 */
data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val employees: List<EmployeeDto>,
)

data class EmployeeDto(
    val id: Long,
    val name: String,
    val teamId: String,
    val records: List<AttendanceRecordDto>,
)

data class AttendanceRecordDto(
    val date: String,
    val status: String,
)

fun EmployeeDto.toDomain(): Employee {
    return Employee(
        id = id,
        name = name,
        teamId = teamId,
        records = records.map { it.toDomain() },
    )
}

fun AttendanceRecordDto.toDomain(): AttendanceRecord {
    return AttendanceRecord(
        date = date,
        status = AttendanceStatus.valueOf(status.uppercase()),
    )
}

interface AttendanceApi {
    suspend fun getSummary(month: String, teamId: String): AttendanceSummaryResponse
}
