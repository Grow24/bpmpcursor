# Attendance Report Feature (TASK-102)

Manager mobile screen for monthly team attendance (present / absent / leave).

## Architecture (MVVM)

```
ui/summary/          ViewModel + Screen + UiState
data/repository/     Team-scoped data access
data/api/            GET /api/v1/attendance/summary
data/auth/           JWT team_id validation
domain/              Employee, AttendanceRecord, summaries
```

## Security

Managers may only view employees whose `team_id` matches the `team_id` claim in their JWT.

## API

`GET /api/v1/attendance/summary?year=YYYY&month=MM`

## Tests

```bash
sh tests/run.sh
```
