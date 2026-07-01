package hr.attendance.viewmodel

import hr.attendance.api.AttendanceApi
import hr.attendance.api.AttendanceSummaryResponse
import hr.attendance.model.AttendanceRecord
import hr.attendance.model.AttendanceStatus
import hr.attendance.repository.AttendanceRepository
import hr.attendance.security.JwtClaims
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AttendanceSummaryViewModelTest {
    @Test
    fun loadMonthlySummary_emitsSuccessForManager() = runTest {
        val claims = JwtClaims(subject = "mgr-1", teamId = "team-alpha", role = "manager")
        val viewModel = AttendanceSummaryViewModel(
            repository = AttendanceRepository(FakeAttendanceApi(listOf(presentRecord()))),
            claims = claims,
            scope = this,
        )

        viewModel.loadMonthlySummary("2025-06")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AttendanceSummaryUiState.Success)
        assertEquals(1, (state as AttendanceSummaryUiState.Success).summary.present)
    }

    @Test
    fun loadMonthlySummary_emitsUnauthorizedForEmployeeRole() = runTest {
        val claims = JwtClaims(subject = "emp-1", teamId = "team-alpha", role = "employee")
        val viewModel = AttendanceSummaryViewModel(
            repository = AttendanceRepository(FakeAttendanceApi(emptyList())),
            claims = claims,
            scope = this,
        )

        viewModel.loadMonthlySummary("2025-06")
        advanceUntilIdle()

        assertEquals(AttendanceSummaryUiState.Unauthorized, viewModel.uiState.value)
    }

    private fun presentRecord(): AttendanceRecord {
        return AttendanceRecord(
            id = "rec-1",
            employeeId = "emp-1",
            teamId = "team-alpha",
            date = LocalDate.parse("2025-06-15"),
            status = AttendanceStatus.PRESENT,
        )
    }

    private class FakeAttendanceApi(
        private val records: List<AttendanceRecord>,
    ) : AttendanceApi {
        override suspend fun fetchSummary(teamId: String, month: String): AttendanceSummaryResponse {
            return AttendanceSummaryResponse(
                month = month,
                teamId = teamId,
                records = records,
            )
        }
    }
}
