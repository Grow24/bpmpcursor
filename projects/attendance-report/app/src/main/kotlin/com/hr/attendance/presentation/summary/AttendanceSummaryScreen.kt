package com.hr.attendance.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hr.attendance.domain.model.AttendanceStatus
import com.hr.attendance.domain.model.StatusGroup

@Composable
fun AttendanceSummaryScreen(
    viewModel: AttendanceSummaryViewModel,
    jwtToken: String,
    month: String,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        AttendanceSummaryUiState.Loading -> LoadingContent()
        is AttendanceSummaryUiState.Error -> ErrorContent(state.message)
        is AttendanceSummaryUiState.Success -> SummaryContent(state.summary.groupedByStatus, state.summary.month)
    }

    androidx.compose.runtime.LaunchedEffect(jwtToken, month) {
        viewModel.loadMonthlySummary(jwtToken = jwtToken, month = month)
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading team attendance…")
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SummaryContent(groups: List<StatusGroup>, month: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Team attendance — $month",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(groups) { group ->
            StatusSummaryCard(group)
        }
    }
}

@Composable
private fun StatusSummaryCard(group: StatusGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = group.status.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = group.count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (group.records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${group.records.size} day(s) recorded",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun AttendanceStatus.label(): String {
    return when (this) {
        AttendanceStatus.PRESENT -> "Present"
        AttendanceStatus.ABSENT -> "Absent"
        AttendanceStatus.LEAVE -> "Leave"
    }
}
