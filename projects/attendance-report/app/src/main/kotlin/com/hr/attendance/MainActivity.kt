package com.hr.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hr.attendance.data.api.HttpAttendanceApi
import com.hr.attendance.data.auth.JwtTeamAuth
import com.hr.attendance.data.repository.AttendanceRepositoryImpl
import com.hr.attendance.presentation.summary.AttendanceSummaryScreen
import com.hr.attendance.presentation.summary.AttendanceSummaryViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiBaseUrl = BuildConfig.API_BASE_URL
        val jwtToken = intent.getStringExtra(EXTRA_JWT).orEmpty()
        val month = intent.getStringExtra(EXTRA_MONTH) ?: currentMonth()

        val viewModel = AttendanceSummaryViewModel(
            teamAuth = JwtTeamAuth(),
            repository = AttendanceRepositoryImpl(HttpAttendanceApi(apiBaseUrl)),
        )

        setContent {
            MaterialTheme {
                Surface {
                    AttendanceSummaryScreen(
                        viewModel = viewModel,
                        jwtToken = jwtToken,
                        month = month,
                    )
                }
            }
        }
    }

    private fun currentMonth(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    companion object {
        const val EXTRA_JWT = "jwt_token"
        const val EXTRA_MONTH = "month"
    }
}
