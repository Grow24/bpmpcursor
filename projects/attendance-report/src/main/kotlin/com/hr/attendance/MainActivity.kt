package com.hr.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hr.attendance.data.AttendanceApiServiceImpl
import com.hr.attendance.data.AttendanceRepository
import com.hr.attendance.data.HttpClient
import com.hr.attendance.ui.AttendanceSummaryScreen
import com.hr.attendance.ui.AttendanceSummaryViewModel

/**
 * Entry point for the HR attendance report mobile app (TASK-102).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authHeader = intent.getStringExtra(EXTRA_AUTH_HEADER).orEmpty()
        val baseUrl = intent.getStringExtra(EXTRA_API_BASE_URL) ?: DEFAULT_API_BASE_URL

        val repository = AttendanceRepository(
            apiService = AttendanceApiServiceImpl(baseUrl, AndroidHttpClient()),
        )
        val viewModel = AttendanceSummaryViewModel(
            repository = repository,
            authHeaderProvider = { authHeader },
        )

        setContent {
            AttendanceSummaryScreen(viewModel = viewModel)
        }
    }

  companion object {
        const val EXTRA_AUTH_HEADER = "auth_header"
        const val EXTRA_API_BASE_URL = "api_base_url"
        const val DEFAULT_API_BASE_URL = "https://api.example.com"
    }
}

/**
 * Platform HTTP client; production builds use OkHttp with the JWT interceptor.
 */
class AndroidHttpClient : HttpClient {
    override suspend fun get(url: String, authHeader: String): String {
        throw UnsupportedOperationException("Wire OkHttp in the app module build.gradle")
    }
}
