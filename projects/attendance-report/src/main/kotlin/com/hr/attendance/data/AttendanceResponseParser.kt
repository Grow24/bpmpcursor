package com.hr.attendance.data

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.Employee
import java.time.LocalDate

object AttendanceResponseParser {
  fun parse(body: String): Pair<List<Employee>, List<AttendanceRecord>> {
    val employees = mutableListOf<Employee>()
    val records = mutableListOf<AttendanceRecord>()

    val employeeBlocks = Regex(""""id"\s*:\s*"([^"]+)"[^}]*"name"\s*:\s*"([^"]+)"[^}]*"team_id"\s*:\s*"([^"]+)"""")
      .findAll(body)

    for (match in employeeBlocks) {
      val (id, name, teamId) = match.destructured
      employees += Employee(id = id, name = name, teamId = teamId)

      val recordPattern = Regex(
        """"employee_id"\s*:\s*"$id"[^}]*"date"\s*:\s*"([^"]+)"[^}]*"status"\s*:\s*"([^"]+)"""",
      )
      for (recordMatch in recordPattern.findAll(body)) {
        val (date, status) = recordMatch.destructured
        records += AttendanceRecord(
          employeeId = id,
          date = LocalDate.parse(date),
          status = AttendanceStatus.valueOf(status.uppercase()),
        )
      }
    }

    return employees to records
  }
}
