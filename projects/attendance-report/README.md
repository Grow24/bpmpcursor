# Attendance Report Feature (TASK-102)

Mobile HR feature: managers view their team's monthly attendance (present / absent / leave).

## Requirement

- **REQ-2025-021** — A manager can view their team's monthly attendance on a mobile screen.

## Architecture (MVVM)

| Layer | Responsibility |
|-------|----------------|
| **Model** | `Employee`, `AttendanceRecord`, `AttendanceSummary` |
| **View** | `AttendanceReportScreen` — renders UI state for the mobile screen |
| **ViewModel** | `AttendanceReportViewModel` — `auth(teamId) → fetchRecords(month) → groupBy(status) → render()` |

## API

```
GET /api/v1/attendance/summary?team_id={teamId}&year={year}&month={month}
Authorization: Bearer <JWT>
```

## Security

Managers may only access data for the `team_id` embedded in their JWT. `TeamAuthGuard` enforces this before any API call.

## Source layout

```
src/main/kotlin/com/hr/attendance/
  domain/       # Employee, AttendanceRecord, AttendanceSummary
  auth/         # JWT decode + team scope guard
  data/         # API client + repository
  viewmodel/    # AttendanceReportViewModel + UiState
  ui/           # AttendanceReportScreen
tests/
  run.sh              # PBMP validation + unit tests
  role_scope_test.py  # JWT team_id scope tests
  viewmodel_test.py   # ViewModel unit tests
```

## Validate locally

```bash
sh tests/run.sh
```
