# Attendance Report Feature (TASK-102)

Manager-facing mobile attendance summary for HR module.

## Requirement (REQ-2025-021)

A manager can view their team's monthly attendance on a mobile screen, with present/absent/leave counts.

## Architecture (MVVM)

```
presentation/   AttendanceSummaryViewModel, AttendanceSummaryUiState, AttendanceReportScreen
data/           AttendanceRepository, AttendanceApiClient  (GET /api/v1/attendance/summary)
domain/         AttendanceRecord, Employee, AttendanceStatus, AttendanceSummary
security/       JwtAuth (team_id from JWT), TeamScopeGuard
```

## Flow

`auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()`

## Security

Managers may only access records scoped to the `team_id` claim in their JWT. Cross-team responses are rejected.

## Tests

```bash
sh tests/run.sh
```

Covers ViewModel grouping and team scope enforcement.
