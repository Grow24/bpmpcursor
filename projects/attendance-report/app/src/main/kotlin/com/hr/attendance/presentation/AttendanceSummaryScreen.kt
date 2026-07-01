package com.hr.attendance.presentation

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hr.attendance.domain.AttendanceStatus

@Composable
fun AttendanceSummaryScreen(
    viewModel: AttendanceSummaryViewModel,
    month: String,
) {
    val uiState by viewModel.uiState.collectAsState()

    AttendanceSummaryContent(
        uiState = uiState,
        onLoad = { viewModel.loadMonthlySummary(month) },
    )
}

@Composable
fun AttendanceSummaryContent(
    uiState: AttendanceSummaryUiState,
    onLoad: () -> Unit,
) {
    if (uiState.isLoading && uiState.employees.isEmpty()) {
        onLoad()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Team Attendance — ${uiState.month}",
            style = MaterialTheme.typography.headlineSmall,
        )

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> {
                SummaryCards(
                    presentCount = uiState.presentCount,
                    absentCount = uiState.absentCount,
                    leaveCount = uiState.leaveCount,
                )
                StatusSection(
                    title = "Present",
                    rows = uiState.groupedByStatus[AttendanceStatus.PRESENT].orEmpty(),
                )
                StatusSection(
                    title = "Absent",
                    rows = uiState.groupedByStatus[AttendanceStatus.ABSENT].orEmpty(),
                )
                StatusSection(
                    title = "Leave",
                    rows = uiState.groupedByStatus[AttendanceStatus.LEAVE].orEmpty(),
                )
            }
        }
    }
}

@Composable
private fun SummaryCards(
    presentCount: Int,
    absentCount: Int,
    leaveCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(label = "Present", count = presentCount, modifier = Modifier.weight(1f))
        SummaryCard(label = "Absent", count = absentCount, modifier = Modifier.weight(1f))
        SummaryCard(label = "Leave", count = leaveCount, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun StatusSection(
    title: String,
    rows: List<EmployeeAttendanceRow>,
) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    if (rows.isEmpty()) {
        Text(text = "No records", style = MaterialTheme.typography.bodySmall)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(rows) { row ->
            Text(text = "${row.employeeName} — ${row.date}")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
