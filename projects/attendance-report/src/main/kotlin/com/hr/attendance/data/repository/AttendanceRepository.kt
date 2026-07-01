package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceSummaryApi
import com.hr.attendance.data.api.AttendanceSummaryItem
import com.hr.attendance.data.auth.JwtTeamAuth
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.domain.EmployeeAttendanceSummary
import java.time.YearMonth

/**
 * Fetches monthly attendance and enforces team scope from the JWT.
 *
 * Pseudocode: auth(teamId) -> fetchRecords(month) -> groupBy(status)
 */
class AttendanceRepository(
    private val api: AttendanceSummaryApi,
    private val jwtTeamAuth: JwtTeamAuth,
) {
    fun loadMonthlySummary(
        authToken: String,
        month: YearMonth,
    ): List<EmployeeAttendanceSummary> {
        val managerTeamId = jwtTeamAuth.teamIdFromToken(authToken)
            ?: throw UnauthorizedTeamAccessException("Missing team_id in JWT token")

        val items = api.fetchSummary(authToken, month)
        return items
            .filter { item -> item.employee.teamId == managerTeamId }
            .map { item -> toSummary(item) }
    }

    private fun toSummary(item: AttendanceSummaryItem): EmployeeAttendanceSummary =
        EmployeeAttendanceSummary.fromRecords(item.employee, item.records)
}
