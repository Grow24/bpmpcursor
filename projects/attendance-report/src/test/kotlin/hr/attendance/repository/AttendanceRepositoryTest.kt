package hr.attendance.repository

import hr.attendance.api.AttendanceApi
import hr.attendance.api.AttendanceSummaryResponse
import hr.attendance.model.AttendanceRecord
import hr.attendance.model.AttendanceStatus
import hr.attendance.security.JwtClaims
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AttendanceRepositoryTest {
    private val managerClaims = JwtClaims(
        subject = "mgr-1",
        teamId = "team-alpha",
        role = "manager",
    )

    @Test
    fun summarize_groupsRecordsByStatus() {
        val repository = AttendanceRepository(FakeAttendanceApi(emptyList()))
        val records = listOf(
            record("1", AttendanceStatus.PRESENT),
            record("2", AttendanceStatus.PRESENT),
            record("3", AttendanceStatus.ABSENT),
            record("4", AttendanceStatus.LEAVE),
        )

        val summary = repository.summarize("2025-06", "team-alpha", records)

        assertEquals("2025-06", summary.month)
        assertEquals(2, summary.present)
        assertEquals(1, summary.absent)
        assertEquals(1, summary.leave)
        assertEquals(4, summary.totalDays)
    }

    @Test
    fun getMonthlySummary_fetchesOnlyManagerTeam() = runTest {
        val records = listOf(
            record("1", AttendanceStatus.PRESENT),
            record("2", AttendanceStatus.LEAVE),
        )
        val repository = AttendanceRepository(FakeAttendanceApi(records))

        val summary = repository.getMonthlySummary(managerClaims, "2025-06")

        assertEquals("team-alpha", summary.teamId)
        assertEquals(1, summary.present)
        assertEquals(1, summary.leave)
    }

    @Test
    fun getMonthlySummary_rejectsWhenApiReturnsDifferentTeam() = runTest {
        val repository = AttendanceRepository(
            FakeAttendanceApi(records = emptyList(), responseTeamId = "team-other"),
        )

        val error = runCatching {
            repository.getMonthlySummary(managerClaims, "2025-06")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("different team", ignoreCase = true) == true)
    }

    @Test
    fun getMonthlySummary_rejectsEmployeeRole() = runTest {
        val employeeClaims = managerClaims.copy(subject = "emp-1", role = "employee")
        val repository = AttendanceRepository(FakeAttendanceApi(emptyList()))

        assertFailsWith<IllegalArgumentException> {
            repository.getMonthlySummary(employeeClaims, "2025-06")
        }
    }

    private fun record(id: String, status: AttendanceStatus): AttendanceRecord {
        return AttendanceRecord(
            id = id,
            employeeId = "emp-$id",
            teamId = "team-alpha",
            date = LocalDate.parse("2025-06-01"),
            status = status,
        )
    }

    private class FakeAttendanceApi(
        private val records: List<AttendanceRecord>,
        private val responseTeamId: String? = null,
    ) : AttendanceApi {
        override suspend fun fetchSummary(teamId: String, month: String): AttendanceSummaryResponse {
            return AttendanceSummaryResponse(
                month = month,
                teamId = responseTeamId ?: teamId,
                records = records,
            )
        }
    }
}
