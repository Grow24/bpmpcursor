package com.hr.attendance.data.api

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.Employee
import java.time.LocalDate
import java.time.YearMonth

/**
 * Contract for GET /api/v1/attendance/summary.
 */
interface AttendanceSummaryApi {
  fun fetchSummary(
    authToken: String,
    month: YearMonth,
  ): List<AttendanceSummaryItem>
}

data class AttendanceSummaryItem(
  val employee: Employee,
  val records: List<AttendanceRecord>,
)

/**
 * Maps raw API JSON into domain objects. Replace with Retrofit/Ktor in production.
 */
class AttendanceSummaryResponseMapper {
  fun mapEmployees(rawEmployees: List<RawEmployee>): List<AttendanceSummaryItem> =
    rawEmployees.map { raw ->
      AttendanceSummaryItem(
        employee = Employee(
          id = raw.id,
          name = raw.name,
          teamId = raw.teamId,
        ),
        records = raw.records.map { record ->
          AttendanceRecord(
            id = record.id,
            employeeId = raw.id,
            date = LocalDate.parse(record.date),
            status = AttendanceStatus.valueOf(record.status.uppercase()),
          )
        },
      )
    }
}

data class RawEmployee(
  val id: String,
  val name: String,
  val teamId: String,
  val records: List<RawAttendanceRecord>,
)

data class RawAttendanceRecord(
  val id: String,
  val date: String,
  val status: String,
)
