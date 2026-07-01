package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.AttendanceSummary
import com.hr.attendance.security.JwtAuth
import com.hr.attendance.security.TeamScopeGuard
import com.hr.attendance.security.UnauthorizedTeamAccessException

class AttendanceRepository(
    private val apiClient: AttendanceApiClient,
    private val teamScopeGuard: TeamScopeGuard = TeamScopeGuard(),
) {
    fun getMonthlySummary(authToken: String, month: String): AttendanceSummary {
        val claims = JwtAuth.parseToken(authToken)
            ?: throw UnauthorizedTeamAccessException("Invalid or missing JWT token.")
        val teamId = claims.teamId

        val response = apiClient.fetchSummary(month = month, teamId = teamId, authToken = authToken)
        teamScopeGuard.assertTeamAccess(response.teamId, teamId)

        val records = teamScopeGuard.filterByTeam(
            response.records.map { it.toDomain() },
            teamId,
        )
        return buildSummary(month = month, teamId = teamId, records = records)
    }

    fun groupByStatus(records: List<AttendanceRecord>): Map<AttendanceStatus, List<AttendanceRecord>> {
        return records.groupBy { it.status }
    }

    private fun buildSummary(
        month: String,
        teamId: String,
        records: List<AttendanceRecord>,
    ): AttendanceSummary {
        val grouped = groupByStatus(records)
        return AttendanceSummary(
            month = month,
            teamId = teamId,
            presentCount = grouped[AttendanceStatus.PRESENT].orEmpty().size,
            absentCount = grouped[AttendanceStatus.ABSENT].orEmpty().size,
            leaveCount = grouped[AttendanceStatus.LEAVE].orEmpty().size,
            recordsByStatus = grouped,
        )
    }
}
