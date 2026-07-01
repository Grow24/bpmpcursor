package com.hr.attendance.presentation

import com.hr.attendance.domain.AttendanceRecord
import com.hr.attendance.domain.AttendanceStatus

data class AttendanceSummaryUiState(
    val isLoading: Boolean = false,
    val month: String = "",
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val leaveCount: Int = 0,
    val recordsByStatus: Map<AttendanceStatus, List<AttendanceRecord>> = emptyMap(),
    val error: String? = null,
)
