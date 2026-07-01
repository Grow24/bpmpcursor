# Attendance Report Feature (TASK-102)

Mobile HR feature: managers view their team's monthly attendance (present / absent / leave).

## Architecture (MVVM)

```
presentation/summary/   ViewModel + Screen + UiState
domain/model/           Employee, AttendanceRecord, AttendanceStatus
domain/repository/      AttendanceRepository interface
data/remote/            GET /api/v1/attendance/summary
data/auth/              JWT team_id scope validation
data/repository/        Fetch + groupBy(status)
```

## Security

Managers may only access data for the `team_id` carried in their JWT token.
`JwtTeamValidator.requireTeamAccess()` runs before any API call.

## Run tests

```bash
sh tests/run.sh
```
