# Attendance Report Feature (TASK-102)

Kotlin Android app for managers to view their team's monthly attendance (present / absent / leave).

## Architecture (MVVM)

```
presentation/   AttendanceSummaryScreen, AttendanceSummaryViewModel
domain/         Employee, AttendanceRecord, AttendanceGrouper, AttendanceRepository
data/           HttpAttendanceApi, JwtTeamAuth, AttendanceRepositoryImpl
```

Flow: `auth(teamId) -> fetchRecords(month) -> groupBy(status) -> render()`

## API

`GET /api/v1/attendance/summary?month=YYYY-MM&team_id={teamId}`

The `team_id` query parameter is taken from the manager JWT (`team_id` claim). The repository rejects responses for other teams.

## Security

Managers may only view attendance for their own team. `JwtTeamAuth` reads `team_id` from the JWT; `AttendanceRepositoryImpl` enforces team scope on API results.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## PBMP validation

```bash
sh tests/run.sh
```
