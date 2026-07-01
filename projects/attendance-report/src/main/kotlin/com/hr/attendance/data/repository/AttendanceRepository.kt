package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceApiService
import com.hr.attendance.data.auth.JwtClaims
import com.hr.attendance.data.auth.JwtTeamValidator
import com.hr.attendance.data.model.AttendanceSummaryResponse
import com.hr.attendance.data.model.Employee
import com.hr.attendance.domain.AttendanceSummaryGrouper

class AttendanceRepository(
    private val api: AttendanceApiService,
    private val teamValidator: JwtTeamValidator = JwtTeamValidator(),
    private val grouper: AttendanceSummaryGrouper = AttendanceSummaryGrouper(),
    private val employeeDirectory: suspend (String) -> List<Employee>,
) {

    suspend fun getMonthlySummary(
        claims: JwtClaims,
        month: String,
        bearerToken: String,
    ): AttendanceSummaryResponse {
        teamValidator.requireTeamAccess(claims, claims.teamId)

        val records = api.fetchAttendanceRecords(
            teamId = claims.teamId,
            month = month,
            bearerToken = bearerToken,
        )

        val employees = employeeDirectory(claims.teamId)
        return grouper.buildSummary(
            month = month,
            teamId = claims.teamId,
            employees = employees,
            records = records.filter { it.teamId == claims.teamId },
        )
    }
}
