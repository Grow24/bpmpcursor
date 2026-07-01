package com.hr.attendance.data

import com.hr.attendance.auth.TeamAuthGuard
import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.AttendanceSummary
import com.hr.attendance.domain.EmployeeMonthlyAttendance

class AttendanceRepository(
  private val api: AttendanceApi,
  private val authGuard: TeamAuthGuard,
) {
  fun getMonthlySummary(
    jwtToken: String,
    year: Int,
    month: Int,
  ): AttendanceSummary {
    val teamId = authGuard.requireManagerTeamId(jwtToken)
    val (employees, records) = api.fetchSummary(teamId, year, month, jwtToken)

    employees.forEach { employee ->
      authGuard.assertTeamScope(jwtToken, employee.teamId)
    }

    return buildSummary(teamId, year, month, employees, records)
  }

  private fun buildSummary(
    teamId: String,
    year: Int,
    month: Int,
    employees: List<com.hr.attendance.domain.Employee>,
    records: List<AttendanceRecord>,
  ): AttendanceSummary {
    val groupedByEmployee = records.groupBy { it.employeeId }
    val employeeSummaries = employees.map { employee ->
      val employeeRecords = groupedByEmployee[employee.id].orEmpty()
      EmployeeMonthlyAttendance(
        employee = employee,
        presentDays = employeeRecords.count { it.status == AttendanceStatus.PRESENT },
        absentDays = employeeRecords.count { it.status == AttendanceStatus.ABSENT },
        leaveDays = employeeRecords.count { it.status == AttendanceStatus.LEAVE },
        records = employeeRecords,
      )
    }

    return AttendanceSummary(
      year = year,
      month = month,
      teamId = teamId,
      totalPresent = employeeSummaries.sumOf { it.presentDays },
      totalAbsent = employeeSummaries.sumOf { it.absentDays },
      totalLeave = employeeSummaries.sumOf { it.leaveDays },
      employees = employeeSummaries,
    )
  }
}
