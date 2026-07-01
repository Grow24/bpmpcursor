package com.hr.attendance.data.dto

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.Employee

data class AttendanceSummaryResponse(
    val month: String,
    val teamId: String,
    val employees: List<EmployeeDto>,
)

data class EmployeeDto(
    val id: String,
    val name: String,
    val teamId: String,
    val records: List<AttendanceRecordDto>,
)

data class AttendanceRecordDto(
    val id: String,
    val employeeId: String,
    val date: String,
    val status: String,
)

fun AttendanceRecordDto.toDomain(): AttendanceRecord {
    return AttendanceRecord(
        id = id,
        employeeId = employeeId,
        date = date,
        status = AttendanceStatus.valueOf(status.uppercase()),
    )
}

fun EmployeeDto.toDomain(): Employee {
    return Employee(
        id = id,
        name = name,
        teamId = teamId,
        attendanceRecords = records.map { it.toDomain() },
    )
}
