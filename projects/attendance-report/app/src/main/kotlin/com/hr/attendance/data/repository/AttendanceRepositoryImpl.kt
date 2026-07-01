package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceApi
import com.hr.attendance.data.api.toDomain
import com.hr.attendance.data.auth.TeamAccessDeniedException
import com.hr.attendance.domain.model.MonthlyAttendanceSummary
import com.hr.attendance.domain.repository.AttendanceRepository
import com.hr.attendance.domain.usecase.AttendanceGrouper

class AttendanceRepositoryImpl(
    private val api: AttendanceApi,
) : AttendanceRepository {

    override suspend fun fetchMonthlySummary(month: String, teamId: String): MonthlyAttendanceSummary {
        val response = api.getSummary(month = month, teamId = teamId)
        if (response.teamId != teamId) {
            throw TeamAccessDeniedException(
                "API returned team ${response.teamId} but manager is scoped to $teamId.",
            )
        }

        val employees = response.employees
            .filter { employee -> employee.teamId == teamId }
            .map { employee -> employee.toDomain() }

        return AttendanceGrouper.buildSummary(
            month = response.month,
            teamId = teamId,
            employees = employees,
        )
    }
}
