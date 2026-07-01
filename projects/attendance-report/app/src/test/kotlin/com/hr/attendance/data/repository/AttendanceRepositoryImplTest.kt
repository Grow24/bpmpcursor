package com.hr.attendance.data.repository

import com.hr.attendance.data.api.AttendanceApi
import com.hr.attendance.data.api.AttendanceRecordDto
import com.hr.attendance.data.api.AttendanceSummaryResponse
import com.hr.attendance.data.api.EmployeeDto
import com.hr.attendance.data.auth.TeamAccessDeniedException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AttendanceRepositoryImplTest {

    @Test
    fun fetchMonthlySummary_filtersToManagerTeam() = runBlocking {
        val repository = AttendanceRepositoryImpl(FakeAttendanceApi())
        val summary = repository.fetchMonthlySummary(month = "2026-06", teamId = "team-1")

        assertEquals("team-1", summary.teamId)
        assertEquals(1, summary.employees.size)
        assertEquals("Alice", summary.employees.first().name)
    }

    @Test
    fun fetchMonthlySummary_rejectsMismatchedTeamFromApi() {
        val repository = AttendanceRepositoryImpl(
            FakeAttendanceApi(responseTeamId = "team-other"),
        )

        assertThrows(TeamAccessDeniedException::class.java) {
            runBlocking {
                repository.fetchMonthlySummary(month = "2026-06", teamId = "team-1")
            }
        }
    }

    private class FakeAttendanceApi(
        private val responseTeamId: String = "team-1",
    ) : AttendanceApi {
        override suspend fun getSummary(month: String, teamId: String): AttendanceSummaryResponse {
            return AttendanceSummaryResponse(
                month = month,
                teamId = responseTeamId,
                employees = listOf(
                    EmployeeDto(
                        id = 1,
                        name = "Alice",
                        teamId = "team-1",
                        records = listOf(
                            AttendanceRecordDto("2026-06-01", "PRESENT"),
                        ),
                    ),
                    EmployeeDto(
                        id = 2,
                        name = "Eve",
                        teamId = "team-2",
                        records = listOf(
                            AttendanceRecordDto("2026-06-01", "ABSENT"),
                        ),
                    ),
                ),
            )
        }
    }
}
