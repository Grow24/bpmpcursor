package com.hr.attendance.presentation

import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.AttendanceSummary
import com.hr.attendance.domain.Employee

data class AttendanceSummaryUiState(
    val isLoading: Boolean = false,
    val month: String = "",
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val leaveCount: Int = 0,
    val employees: List<Employee> = emptyList(),
    val groupedByStatus: Map<AttendanceStatus, List<EmployeeAttendanceRow>> = emptyMap(),
    val errorMessage: String? = null,
)

data class EmployeeAttendanceRow(
    val employeeName: String,
    val date: String,
    val status: AttendanceStatus,
)

fun AttendanceSummary.toUiState(): AttendanceSummaryUiState {
    val rows = employees.flatMap { employee ->
        employee.attendanceRecords.map { record ->
            EmployeeAttendanceRow(
                employeeName = employee.name,
                date = record.date,
                status = record.status,
            )
        }
    }

    return AttendanceSummaryUiState(
        isLoading = false,
        month = month,
        presentCount = presentCount,
        absentCount = absentCount,
        leaveCount = leaveCount,
        employees = employees,
        groupedByStatus = rows.groupBy { it.status },
        errorMessage = null,
    )
}
