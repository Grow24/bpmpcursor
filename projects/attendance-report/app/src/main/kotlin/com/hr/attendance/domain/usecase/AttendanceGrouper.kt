package com.hr.attendance.domain.usecase

import com.hr.attendance.domain.model.AttendanceRecord
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.Employee
import com.hr.attendance.domain.model.MonthlyAttendanceSummary
import com.hr.attendance.domain.model.StatusGroup

object AttendanceGrouper {

    fun groupByStatus(employees: List<Employee>): List<StatusGroup> {
        val allRecords = employees.flatMap { employee ->
            employee.records.map { record ->
                record to employee.name
            }
        }

        return AttendanceStatus.entries.map { status ->
            val matching = allRecords.filter { (record, _) -> record.status == status }
            StatusGroup(
                status = status,
                count = matching.size,
                records = matching.map { (record, _) -> record },
            )
        }
    }

    fun buildSummary(month: String, teamId: String, employees: List<Employee>): MonthlyAttendanceSummary {
        return MonthlyAttendanceSummary(
            month = month,
            teamId = teamId,
            employees = employees,
            groupedByStatus = groupByStatus(employees),
        )
    }
}
