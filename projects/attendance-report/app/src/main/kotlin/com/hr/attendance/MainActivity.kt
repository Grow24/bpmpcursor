package com.hr.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.hr.attendance.data.auth.JwtTokenProviderImpl
import com.hr.attendance.data.repository.AttendanceRepository
import com.hr.attendance.presentation.AttendanceSummaryScreen
import com.hr.attendance.presentation.AttendanceSummaryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = intent.getStringExtra(EXTRA_JWT_TOKEN)
        val month = intent.getStringExtra(EXTRA_MONTH) ?: DEFAULT_MONTH
        val repository = AttendanceRepository(
            api = AttendanceApp.api,
            jwtTokenProvider = JwtTokenProviderImpl(token),
        )
        val viewModel = AttendanceSummaryViewModel(repository)

        setContent {
            MaterialTheme {
                AttendanceSummaryScreen(viewModel = viewModel, month = month)
            }
        }
    }

    companion object {
        const val EXTRA_JWT_TOKEN = "jwt_token"
        const val EXTRA_MONTH = "month"
        const val DEFAULT_MONTH = "2026-06"
    }
}
