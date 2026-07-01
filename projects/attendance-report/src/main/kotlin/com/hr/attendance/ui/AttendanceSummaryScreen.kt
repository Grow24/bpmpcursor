package com.hr.attendance.ui

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
import com.hr.attendance.data.EmployeeAttendanceRow
import com.hr.attendance.model.AttendanceStatus

/**
 * Mobile screen: monthly team attendance with present / absent / leave counts.
 */
@Composable
fun AttendanceSummaryScreen(viewModel: AttendanceSummaryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        AttendanceSummaryUiState.Loading -> LoadingContent()
        is AttendanceSummaryUiState.Error -> ErrorContent(state.message)
        is AttendanceSummaryUiState.Success -> SuccessContent(state)
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
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading attendance…")
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
private fun SuccessContent(state: AttendanceSummaryUiState.Success) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Team Attendance — ${state.month}",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusTotalsRow(state.statusTotals)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.employees, key = { it.employeeId }) { row ->
                EmployeeAttendanceCard(row)
            }
        }
    }
}

@Composable
private fun StatusTotalsRow(totals: Map<AttendanceStatus, Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatusChip(label = "Present", count = totals[AttendanceStatus.PRESENT] ?: 0)
        StatusChip(label = "Absent", count = totals[AttendanceStatus.ABSENT] ?: 0)
        StatusChip(label = "Leave", count = totals[AttendanceStatus.LEAVE] ?: 0)
    }
}

@Composable
private fun StatusChip(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmployeeAttendanceCard(row: EmployeeAttendanceRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = row.employeeName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Present ${row.counts.present} · Absent ${row.counts.absent} · Leave ${row.counts.leave}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
