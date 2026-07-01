package com.hr.attendance.data.api

import com.hr.attendance.data.model.AttendanceRecord

/**
 * REST contract: GET /api/v1/attendance/summary
 */
interface AttendanceApiService {

  suspend fun fetchAttendanceRecords(
      teamId: String,
      month: String,
      bearerToken: String,
  ): List<AttendanceRecord>
}
