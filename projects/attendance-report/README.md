# Attendance Report Feature (TASK-102)

Manager mobile screen for monthly team attendance (present / absent / leave).

## Architecture (MVVM)

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Model | `model/` | `Employee`, `AttendanceRecord`, `AttendanceStatus` |
| Security | `security/` | JWT `team_id` extraction and manager team scope |
| Data | `data/` | `GET /api/v1/attendance/summary` client and repository |
| UI | `ui/` | `AttendanceSummaryViewModel`, Compose screen |

Flow: `auth(teamId)` → `fetchRecords(month)` → `groupBy(status)` → render.

## Security

Managers may only view their own team. `TeamScopeValidator` reads `team_id` from the JWT
and rejects responses whose `teamId` does not match.

## Tests

```bash
sh tests/run.sh
```
