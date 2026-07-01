package com.hr.attendance.data.repository

import com.hr.attendance.data.auth.JwtTeamValidator
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.remote.AttendanceApiService
import com.hr.attendance.data.remote.dto.AttendanceSummaryResponse
import com.hr.attendance.domain.model.AttendanceRecord
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.Employee
import com.hr.attendance.domain.model.EmployeeAttendanceSummary
import com.hr.attendance.domain.model.TeamAttendanceSummary
import com.hr.attendance.domain.repository.AttendanceRepository
import java.time.LocalDate

class AttendanceRepositoryImpl(
    private val api: AttendanceApiService,
    private val jwtTeamValidator: JwtTeamValidator,
    private val tokenProvider: suspend () -> String,
    private val jwtPayloadProvider: suspend () -> Map<String, String>,
) : AttendanceRepository {

    override suspend fun fetchMonthlySummary(
        teamId: String,
        month: Int,
        year: Int,
    ): TeamAttendanceSummary {
        val payload = jwtPayloadProvider()
        val tokenTeamId = jwtTeamValidator.extractTeamId(payload)
        jwtTeamValidator.requireTeamAccess(tokenTeamId, teamId)

        val token = tokenProvider()
        val response = api.getSummary(teamId, month, year, token)
        return mapResponse(response)
    }

    internal fun mapResponse(response: AttendanceSummaryResponse): TeamAttendanceSummary {
        val employees = response.employees.map { dto ->
            Employee(id = dto.id, name = dto.name, teamId = dto.teamId)
        }.associateBy { it.id }

        val records = response.records.map { dto ->
            AttendanceRecord(
                employeeId = dto.employeeId,
                date = LocalDate.parse(dto.date),
                status = response.parseStatus(dto.status),
            )
        }

        val grouped = groupByStatus(records, employees)
        return TeamAttendanceSummary(
            teamId = response.teamId,
            month = response.month,
            year = response.year,
            employees = grouped,
        )
    }

    internal fun groupByStatus(
        records: List<AttendanceRecord>,
        employees: Map<String, Employee>,
    ): List<EmployeeAttendanceSummary> {
        return records
            .groupBy { it.employeeId }
            .map { (employeeId, employeeRecords) ->
                val employee = employees[employeeId]
                    ?: throw UnauthorizedTeamAccessException(
                        "Employee $employeeId is not in the manager's team.",
                    )
                EmployeeAttendanceSummary(
                    employee = employee,
                    presentDays = employeeRecords.count { it.status == AttendanceStatus.PRESENT },
                    absentDays = employeeRecords.count { it.status == AttendanceStatus.ABSENT },
                    leaveDays = employeeRecords.count { it.status == AttendanceStatus.LEAVE },
                )
            }
            .sortedBy { it.employee.name }
    }
}
