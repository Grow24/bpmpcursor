package com.hr.attendance.security

import com.hr.attendance.domain.AttendanceRecord

class TeamScopeGuard {
    fun assertTeamAccess(requestedTeamId: String, jwtTeamId: String) {
        if (requestedTeamId != jwtTeamId) {
            throw UnauthorizedTeamAccessException()
        }
    }

    fun filterByTeam(records: List<AttendanceRecord>, teamId: String): List<AttendanceRecord> {
        return records.filter { it.teamId == teamId }
    }
}
