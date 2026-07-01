package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceApiService
import com.hr.attendance.data.auth.JwtTokenProvider
import com.hr.attendance.data.auth.UnauthorizedTeamAccessException
import com.hr.attendance.data.dto.AttendanceRecordDto
import com.hr.attendance.data.dto.AttendanceSummaryResponse
import com.hr.attendance.data.dto.EmployeeDto
import com.hr.attendance.domain.AttendanceStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceRepositoryTest {

    private val api: AttendanceApiService = mockk()
    private val jwtTokenProvider: JwtTokenProvider = mockk()

    private val repository = AttendanceRepository(api, jwtTokenProvider)

    @Test
    fun fetchMonthlySummary_usesTeamIdFromJwt() = runTest {
        every { jwtTokenProvider.teamId() } returns "team-alpha"
        coEvery { api.getSummary("2026-06", "team-alpha") } returns sampleResponse("team-alpha")

        val summary = repository.fetchMonthlySummary("2026-06")

        assertEquals("team-alpha", summary.teamId)
        assertEquals(1, summary.presentCount)
        assertEquals(1, summary.absentCount)
        assertEquals(1, summary.leaveCount)
    }

    @Test
    fun fetchMonthlySummary_rejectsMismatchedTeam() = runTest {
        every { jwtTokenProvider.teamId() } returns "team-alpha"
        coEvery { api.getSummary("2026-06", "team-alpha") } returns sampleResponse("team-beta")

        val error = runCatching { repository.fetchMonthlySummary("2026-06") }.exceptionOrNull()

        assertTrue(error is UnauthorizedTeamAccessException)
    }

    @Test
    fun fetchMonthlySummary_rejectsMissingTeamId() = runTest {
        every { jwtTokenProvider.teamId() } returns null

        val error = runCatching { repository.fetchMonthlySummary("2026-06") }.exceptionOrNull()

        assertTrue(error is UnauthorizedTeamAccessException)
    }

    @Test
    fun groupByStatus_groupsRecords() {
        val records = listOf(
            sampleRecord("1", AttendanceStatus.PRESENT),
            sampleRecord("2", AttendanceStatus.PRESENT),
            sampleRecord("3", AttendanceStatus.ABSENT),
        )

        val grouped = repository.groupByStatus(records)

        assertEquals(2, grouped[AttendanceStatus.PRESENT]?.size)
        assertEquals(1, grouped[AttendanceStatus.ABSENT]?.size)
    }

    private fun sampleResponse(teamId: String): AttendanceSummaryResponse {
        return AttendanceSummaryResponse(
            month = "2026-06",
            teamId = teamId,
            employees = listOf(
                EmployeeDto(
                    id = "e1",
                    name = "Alex",
                    teamId = teamId,
                    records = listOf(
                        AttendanceRecordDto("r1", "e1", "2026-06-01", "PRESENT"),
                        AttendanceRecordDto("r2", "e1", "2026-06-02", "ABSENT"),
                        AttendanceRecordDto("r3", "e1", "2026-06-03", "LEAVE"),
                    ),
                ),
            ),
        )
    }

    private fun sampleRecord(id: String, status: AttendanceStatus) =
        com.hr.attendance.domain.AttendanceRecord(
            id = id,
            employeeId = "e1",
            date = "2026-06-01",
            status = status,
        )
}
