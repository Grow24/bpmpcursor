package com.hr.attendance.presentation

import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.repository.AttendanceRepository
import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus
import com.hr.attendance.domain.AttendanceSummary
import com.hr.attendance.domain.Employee
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: AttendanceRepository = mockk()
    private lateinit var viewModel: AttendanceSummaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AttendanceSummaryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMonthlySummary_rendersGroupedCounts() = runTest {
        coEvery { repository.fetchMonthlySummary("2026-06") } returns sampleSummary()

        viewModel.loadMonthlySummary("2026-06")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("2026-06", state.month)
        assertEquals(1, state.presentCount)
        assertEquals(1, state.absentCount)
        assertEquals(1, state.leaveCount)
        assertNotNull(state.groupedByStatus[AttendanceStatus.PRESENT])
    }

    @Test
    fun loadMonthlySummary_surfacesUnauthorizedError() = runTest {
        coEvery { repository.fetchMonthlySummary("2026-06") } throws
            UnauthorizedTeamAccessException()

        viewModel.loadMonthlySummary("2026-06")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    private fun sampleSummary(): AttendanceSummary {
        val employee = Employee(
            id = "e1",
            name = "Alex",
            teamId = "team-alpha",
            attendanceRecords = listOf(
                AttendanceRecord("r1", "e1", "2026-06-01", AttendanceStatus.PRESENT),
                AttendanceRecord("r2", "e1", "2026-06-02", AttendanceStatus.ABSENT),
                AttendanceRecord("r3", "e1", "2026-06-03", AttendanceStatus.LEAVE),
            ),
        )
        return AttendanceSummary(
            month = "2026-06",
            teamId = "team-alpha",
            presentCount = 1,
            absentCount = 1,
            leaveCount = 1,
            employees = listOf(employee),
        )
    }
}
