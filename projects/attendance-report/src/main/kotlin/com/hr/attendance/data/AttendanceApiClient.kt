package com.hr.attendance.data

interface AttendanceApiClient {
    /**
     * GET /api/v1/attendance/summary?month={month}&team_id={teamId}
     */
    fun fetchSummary(month: String, teamId: String, authToken: String): AttendanceSummaryResponse
}
