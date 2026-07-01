package com.hr.attendance.presentation.summary

import com.hr.attendance.domain.model.AttendanceRecord
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.Employee
import com.hr.attendance.domain.repository.AttendanceRepository
import com.hr.attendance.domain.model.TeamAttendanceSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSummaryViewModelTest {

    @Test
    fun `loadMonthlySummary emits success with status totals`() = runTest {
        val summary = TeamAttendanceSummary(
            teamId = "team-1",
            month = 6,
            year = 2026,
            employees = emptyList(),
        )
        val repository = FakeAttendanceRepository(summary)
        val viewModel = AttendanceSummaryViewModel(
            repository = repository,
            managerTeamId = "team-1",
            scope = this,
        )

        viewModel.loadMonthlySummary(month = 6, year = 2026)

        val state = viewModel.uiState.value
        assertTrue(state is AttendanceSummaryUiState.Success)
    }

    private class FakeAttendanceRepository(
        private val summary: TeamAttendanceSummary,
    ) : AttendanceRepository {
        override suspend fun fetchMonthlySummary(
            teamId: String,
            month: Int,
            year: Int,
        ): TeamAttendanceSummary = summary
    }
}

class AttendanceRecordGroupingTest {

    @Test
    fun `groupBy status counts present absent and leave days`() {
        val employee = Employee(id = "e1", name = "Alice", teamId = "team-1")
        val records = listOf(
            AttendanceRecord(employee.id, java.time.LocalDate.of(2026, 6, 1), AttendanceStatus.PRESENT),
            AttendanceRecord(employee.id, java.time.LocalDate.of(2026, 6, 2), AttendanceStatus.ABSENT),
            AttendanceRecord(employee.id, java.time.LocalDate.of(2026, 6, 3), AttendanceStatus.LEAVE),
        )
        val totals = StatusTotals.from(
            listOf(
                com.hr.attendance.domain.model.EmployeeAttendanceSummary(
                    employee = employee,
                    presentDays = 1,
                    absentDays = 1,
                    leaveDays = 1,
                ),
            ),
        )
        assertEquals(1, totals.present)
        assertEquals(1, totals.absent)
        assertEquals(1, totals.leave)
    }
}
