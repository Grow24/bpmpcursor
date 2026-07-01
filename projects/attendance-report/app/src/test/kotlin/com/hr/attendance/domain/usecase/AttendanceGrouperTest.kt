package com.hr.attendance.domain.usecase

import com.hr.attendance.domain.model.AttendanceRecord
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.Employee
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceGrouperTest {

    @Test
    fun groupByStatus_countsPresentAbsentAndLeave() {
        val employees = listOf(
            Employee(
                id = 1,
                name = "Alice",
                teamId = "team-1",
                records = listOf(
                    AttendanceRecord("2026-06-01", AttendanceStatus.PRESENT),
                    AttendanceRecord("2026-06-02", AttendanceStatus.ABSENT),
                ),
            ),
            Employee(
                id = 2,
                name = "Bob",
                teamId = "team-1",
                records = listOf(
                    AttendanceRecord("2026-06-01", AttendanceStatus.LEAVE),
                    AttendanceRecord("2026-06-02", AttendanceStatus.PRESENT),
                ),
            ),
        )

        val groups = AttendanceGrouper.groupByStatus(employees)

        assertEquals(2, groups.first { it.status == AttendanceStatus.PRESENT }.count)
        assertEquals(1, groups.first { it.status == AttendanceStatus.ABSENT }.count)
        assertEquals(1, groups.first { it.status == AttendanceStatus.LEAVE }.count)
    }
}
