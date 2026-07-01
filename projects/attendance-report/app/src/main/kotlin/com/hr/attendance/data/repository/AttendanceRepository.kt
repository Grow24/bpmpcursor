package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceApiService
import com.hr.attendance.data.auth.JwtTokenProvider
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.dto.toDomain
import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.AttendanceSummary
import com.hr.attendance.domain.Employee

class AttendanceRepository(
    private val api: AttendanceApiService,
    private val jwtTokenProvider: JwtTokenProvider,
) {

    suspend fun fetchMonthlySummary(month: String): AttendanceSummary {
        val teamId = jwtTokenProvider.teamId()
            ?: throw UnauthorizedTeamAccessException("Missing team_id in JWT token")

        val response = api.getSummary(month = month, teamId = teamId)
        if (response.teamId != teamId) {
            throw UnauthorizedTeamAccessException(
                "Requested team ${response.teamId} does not match token team $teamId",
            )
        }

        val employees = response.employees.map { it.toDomain() }
        val records = employees.flatMap(Employee::attendanceRecords)

        return AttendanceSummary(
            month = response.month,
            teamId = response.teamId,
            presentCount = records.count { it.status == AttendanceStatus.PRESENT },
            absentCount = records.count { it.status == AttendanceStatus.ABSENT },
            leaveCount = records.count { it.status == AttendanceStatus.LEAVE },
            employees = employees,
        )
    }

    fun groupByStatus(records: List<AttendanceRecord>): Map<AttendanceStatus, List<AttendanceRecord>> {
        return records.groupBy { it.status }
    }
}
