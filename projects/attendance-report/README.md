# Attendance Report Feature (TASK-102)

Kotlin MVVM mobile feature for managers to view their team's monthly attendance summary (present / absent / leave).

## API

- `GET /api/v1/attendance/summary?month=YYYY-MM`
- Scoped by `team_id` from the manager's JWT token.

## Architecture

```
auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()
```

- **Model:** `AttendanceRecord`, `Employee`, `AttendanceSummary`
- **Security:** `TeamScopeGuard` validates JWT `team_id` and manager role
- **Repository:** `AttendanceRepository` calls the API and aggregates counts
- **ViewModel:** `AttendanceSummaryViewModel` exposes `AttendanceSummaryUiState`
- **UI:** `AttendanceSummaryScreenModel` maps state for the mobile screen

## Run tests

```bash
sh tests/run.sh
```
