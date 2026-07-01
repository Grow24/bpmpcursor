package com.hr.attendance

import com.hr.attendance.data.auth.JwtTeamValidator
import com.hr.attendance.data.remote.AttendanceApiServiceImpl
import com.hr.attendance.data.repository.AttendanceRepositoryImpl
import com.hr.attendance.presentation.summary.AttendanceSummaryViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Application entry point wiring MVVM layers for the attendance report feature.
 */
object AttendanceReportApp {

    fun createViewModel(
        baseUrl: String,
        managerTeamId: String,
        tokenProvider: suspend () -> String,
        jwtPayloadProvider: suspend () -> Map<String, String>,
        httpClient: suspend (String, Map<String, String>) -> String,
        scope: CoroutineScope,
    ): AttendanceSummaryViewModel {
        val api = AttendanceApiServiceImpl(baseUrl, httpClient)
        val repository = AttendanceRepositoryImpl(
            api = api,
            jwtTeamValidator = JwtTeamValidator(),
            tokenProvider = tokenProvider,
            jwtPayloadProvider = jwtPayloadProvider,
        )
        return AttendanceSummaryViewModel(
            repository = repository,
            managerTeamId = managerTeamId,
            scope = scope,
        )
    }
}
