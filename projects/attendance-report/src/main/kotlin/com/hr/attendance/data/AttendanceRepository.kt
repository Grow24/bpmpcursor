package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.Employee
import com.hr.attendance.domain.EmployeeAttendanceSummary
import com.hr.attendance.domain.TeamAttendanceSummary
import java.time.LocalDate
import java.time.YearMonth
import java.util.Base64

class AttendanceRepository(
    private val api: AttendanceApi,
) {

    suspend fun getTeamMonthlySummary(
        bearerToken: String,
        month: YearMonth,
    ): TeamAttendanceSummary {
        val tokenTeamId = JwtTeamScope.teamIdFromToken(bearerToken)
            ?: throw JwtTeamScope.UnauthorizedTeamAccessException("Missing or invalid team_id in JWT")

        val monthKey = month.toString()
        val response = api.fetchSummary(monthKey, tokenTeamId)
        JwtTeamScope.requireTeamAccess(tokenTeamId, response.teamId)

        val recordsInMonth = response.records.filter { record ->
            YearMonth.from(record.date) == month
        }

        val summaries = response.employees.map { employee ->
            val employeeRecords = recordsInMonth.filter { it.employeeId == employee.id }
            toEmployeeSummary(employee, employeeRecords)
        }

        return TeamAttendanceSummary(
            month = monthKey,
            teamId = response.teamId,
            employees = summaries,
        )
    }

    private fun toEmployeeSummary(
        employee: Employee,
        records: List<AttendanceRecord>,
    ): EmployeeAttendanceSummary {
        val grouped = records.groupingBy { it.status }.eachCount()
        return EmployeeAttendanceSummary(
            employeeId = employee.id,
            employeeName = employee.name,
            present = grouped[AttendanceStatus.PRESENT] ?: 0,
            absent = grouped[AttendanceStatus.ABSENT] ?: 0,
            leave = grouped[AttendanceStatus.LEAVE] ?: 0,
        )
    }
}

/**
 * In-memory API stub for development and unit tests.
 */
class InMemoryAttendanceApi(
    private val teamEmployees: Map<String, List<Employee>>,
    private val teamRecords: Map<String, List<AttendanceRecord>>,
) : AttendanceApi {

    override suspend fun fetchSummary(month: String, teamId: String): AttendanceSummaryResponse {
        return AttendanceSummaryResponse(
            month = month,
            teamId = teamId,
            employees = teamEmployees[teamId].orEmpty(),
            records = teamRecords[teamId].orEmpty(),
        )
    }
}

object AttendanceSampleData {

    private const val TEAM_ALPHA = "team-alpha"

    fun sampleBearerToken(teamId: String = TEAM_ALPHA): String {
        val header = base64Url("""{"alg":"HS256","typ":"JWT"}""")
        val payload = base64Url("""{"sub":"mgr-1","role":"manager","team_id":"$teamId"}""")
        return "Bearer $header.$payload.signature"
    }

    fun sampleApi(): InMemoryAttendanceApi {
        val employees = listOf(
            Employee(id = "e1", name = "Alice Kumar", teamId = TEAM_ALPHA),
            Employee(id = "e2", name = "Bob Singh", teamId = TEAM_ALPHA),
        )
        val records = listOf(
            AttendanceRecord("e1", LocalDate.parse("2026-06-02"), AttendanceStatus.PRESENT),
            AttendanceRecord("e1", LocalDate.parse("2026-06-03"), AttendanceStatus.ABSENT),
            AttendanceRecord("e1", LocalDate.parse("2026-06-04"), AttendanceStatus.LEAVE),
            AttendanceRecord("e2", LocalDate.parse("2026-06-02"), AttendanceStatus.PRESENT),
            AttendanceRecord("e2", LocalDate.parse("2026-06-03"), AttendanceStatus.PRESENT),
        )
        return InMemoryAttendanceApi(
            teamEmployees = mapOf(TEAM_ALPHA to employees),
            teamRecords = mapOf(TEAM_ALPHA to records),
        )
    }

    private fun base64Url(value: String): String {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))
    }
}
