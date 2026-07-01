package com.hr.attendance.data

import com.hr.attendance.model.AttendanceRecord
import com.hr.attendance.model.AttendanceStatus
import com.hr.attendance.model.AttendanceSummaryResponse
import com.hr.attendance.model.EmployeeAttendanceSummary
import com.hr.attendance.model.StatusCounts
import com.hr.attendance.security.TeamScopeValidator
import java.time.YearMonth

/**
 * Repository: auth(teamId) -> fetchRecords(month) per approved pseudocode.
 */
class AttendanceRepository(
    private val apiService: AttendanceApiService,
    private val teamScopeValidator: TeamScopeValidator = TeamScopeValidator(),
) {

    suspend fun loadMonthlySummary(
        month: YearMonth,
        authHeader: String,
    ): MonthlyAttendanceSummary {
        val authorizedTeamId = teamScopeValidator.authorizeManagerAccess(authHeader)
        val response = apiService.fetchSummary(month, authHeader)
        teamScopeValidator.assertTeamScope(authorizedTeamId, response.teamId)
        return response.toMonthlySummary()
    }
}

data class MonthlyAttendanceSummary(
    val month: YearMonth,
    val teamId: String,
    val employees: List<EmployeeAttendanceRow>,
    val groupedByStatus: Map<AttendanceStatus, List<AttendanceRecord>>,
)

data class EmployeeAttendanceRow(
    val employeeId: String,
    val employeeName: String,
    val counts: StatusCounts,
    val records: List<AttendanceRecord>,
)

fun AttendanceSummaryResponse.toMonthlySummary(): MonthlyAttendanceSummary {
    val rows = employees.map { it.toRow() }
    val allRecords = employees.flatMap { it.records }
    return MonthlyAttendanceSummary(
        month = YearMonth.parse(month),
        teamId = teamId,
        employees = rows,
        groupedByStatus = allRecords.groupBy { it.status },
    )
}

private fun EmployeeAttendanceSummary.toRow(): EmployeeAttendanceRow {
    val counts = records.fold(StatusCounts()) { acc, record ->
        when (record.status) {
            AttendanceStatus.PRESENT -> acc.copy(present = acc.present + 1)
            AttendanceStatus.ABSENT -> acc.copy(absent = acc.absent + 1)
            AttendanceStatus.LEAVE -> acc.copy(leave = acc.leave + 1)
        }
    }
    return EmployeeAttendanceRow(
        employeeId = employee.id,
        employeeName = employee.name,
        counts = counts,
        records = records,
    )
}
