package com.hr.attendance.domain

import com.hr.attendance.data.model.AttendanceRecord
import com.hr.attendance.data.model.AttendanceStatus
import com.hr.attendance.data.model.Employee
import java.time.LocalDate

/**
 * ViewModel / role scope test fixtures (TASK-102).
 */
object AttendanceTestFixtures {

    const val TEAM_A = "team-a"
    const val TEAM_B = "team-b"

    val employeesTeamA = listOf(
        Employee("e1", "Alice", TEAM_A),
        Employee("e2", "Bob", TEAM_A),
    )

    fun recordsTeamA(): List<AttendanceRecord> = listOf(
        AttendanceRecord("r1", "e1", LocalDate.parse("2026-06-01"), AttendanceStatus.PRESENT, TEAM_A),
        AttendanceRecord("r2", "e1", LocalDate.parse("2026-06-02"), AttendanceStatus.ABSENT, TEAM_A),
        AttendanceRecord("r3", "e2", LocalDate.parse("2026-06-01"), AttendanceStatus.LEAVE, TEAM_A),
    )

    fun expectedSummaryPresentCount(): Int = 1
}
