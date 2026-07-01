package com.hr.attendance.domain.repository

import com.hr.attendance.domain.model.MonthlyAttendanceSummary

interface AttendanceRepository {
    suspend fun fetchMonthlySummary(month: String, teamId: String): MonthlyAttendanceSummary
}
