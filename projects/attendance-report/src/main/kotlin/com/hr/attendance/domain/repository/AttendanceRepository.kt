package com.hr.attendance.domain.repository

import com.hr.attendance.domain.model.TeamAttendanceSummary

interface AttendanceRepository {
    suspend fun fetchMonthlySummary(
        teamId: String,
        month: Int,
        year: Int,
    ): TeamAttendanceSummary
}
