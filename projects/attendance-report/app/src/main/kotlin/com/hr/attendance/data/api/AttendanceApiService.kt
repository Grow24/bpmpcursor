package com.hr.attendance.data.api

import com.hr.attendance.data.dto.AttendanceSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AttendanceApiService {
    @GET("/api/v1/attendance/summary")
    suspend fun getSummary(
        @Query("month") month: String,
        @Query("team_id") teamId: String,
    ): AttendanceSummaryResponse
}
