package hr.attendance.repository

import hr.attendance.api.AttendanceApi
import hr.attendance.model.AttendanceRecord
import hr.attendance.model.AttendanceStatus
import hr.attendance.model.AttendanceSummary
import hr.attendance.security.JwtClaims
import hr.attendance.security.TeamScopeGuard

class AttendanceRepository(
    private val api: AttendanceApi,
    private val teamScopeGuard: TeamScopeGuard = TeamScopeGuard(),
) {
    suspend fun getMonthlySummary(
        claims: JwtClaims,
        month: String,
    ): AttendanceSummary {
        teamScopeGuard.authorize(claims, claims.teamId)

        val response = api.fetchSummary(claims.teamId, month)
        require(response.teamId == claims.teamId) {
            "API returned data for a different team"
        }

        return summarize(month, response.teamId, response.records)
    }

    fun summarize(
        month: String,
        teamId: String,
        records: List<AttendanceRecord>,
    ): AttendanceSummary {
        val grouped = records.groupingBy { it.status }.eachCount()
        return AttendanceSummary(
            month = month,
            teamId = teamId,
            present = grouped[AttendanceStatus.PRESENT] ?: 0,
            absent = grouped[AttendanceStatus.ABSENT] ?: 0,
            leave = grouped[AttendanceStatus.LEAVE] ?: 0,
        )
    }
}
