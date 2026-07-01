package com.hr.attendance.presentation.summary

import com.hr.attendance.data.auth.JwtTeamAuth
import com.hr.attendance.data.repository.AttendanceRepositoryImpl
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.MonthlyAttendanceSummary
import com.hr.attendance.domain.repository.AttendanceRepository
import com.hr.attendance.domain.usecase.AttendanceGrouper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceSummaryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMonthlySummary_emitsGroupedSummary() = runTest(dispatcher) {
        val repository = object : AttendanceRepository {
            override suspend fun fetchMonthlySummary(month: String, teamId: String): MonthlyAttendanceSummary {
                return AttendanceGrouper.buildSummary(month, teamId, emptyList())
            }
        }
        val viewModel = AttendanceSummaryViewModel(JwtTeamAuth(), repository)
        val token = jwt(teamId = "team-1")

        viewModel.loadMonthlySummary(jwtToken = token, month = "2026-06")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AttendanceSummaryUiState.Success)
        val summary = (state as AttendanceSummaryUiState.Success).summary
        assertEquals("2026-06", summary.month)
        assertEquals(AttendanceStatus.entries.size, summary.groupedByStatus.size)
    }

    private fun jwt(teamId: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"mgr","team_id":"$teamId"}""".toByteArray())
        return "$header.$payload.sig"
    }
}
