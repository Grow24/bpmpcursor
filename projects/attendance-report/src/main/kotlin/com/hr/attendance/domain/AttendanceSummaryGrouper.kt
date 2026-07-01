package com.hr.attendance.domain

import com.hr.attendance.data.model.AttendanceRecord
import com.hr.attendance.data.model.AttendanceStatus
import com.hr.attendance.data.model.AttendanceSummaryResponse
import com.hr.attendance.data.model.AttendanceTotals
import com.hr.attendance.data.model.Employee
import com.hr.attendance.data.model.EmployeeAttendanceSummary

/**
 * Pseudocode step: groupBy(status) -> monthly summary per employee.
 */
class AttendanceSummaryGrouper {

    fun buildSummary(
        month: String,
        teamId: String,
        employees: List<Employee>,
        records: List<AttendanceRecord>,
    ): AttendanceSummaryResponse {
        val recordsByEmployee = records.groupBy { it.employeeId }
        val employeeSummaries = employees.map { employee ->
            val counts = countByStatus(recordsByEmployee[employee.id].orEmpty())
            EmployeeAttendanceSummary(
                employeeId = employee.id,
                name = employee.name,
                present = counts.present,
                absent = counts.absent,
                leave = counts.leave,
            )
        }

        return AttendanceSummaryResponse(
            month = month,
            teamId = teamId,
            employees = employeeSummaries,
            totals = AttendanceTotals(
                present = employeeSummaries.sumOf { it.present },
                absent = employeeSummaries.sumOf { it.absent },
                leave = employeeSummaries.sumOf { it.leave },
            ),
        )
    }

    private fun countByStatus(records: List<AttendanceRecord>): StatusCounts {
        var present = 0
        var absent = 0
        var leave = 0
        for (record in records) {
            when (record.status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.LEAVE -> leave++
            }
        }
        return StatusCounts(present, absent, leave)
    }

    private data class StatusCounts(
        val present: Int,
        val absent: Int,
        val leave: Int,
    )
}
