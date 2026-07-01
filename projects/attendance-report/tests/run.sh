#!/bin/sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin/com/hr/attendance"

require_file() {
  if [ ! -f "$1" ]; then
    echo "Missing required file: $1"
    exit 1
  fi
}

require_pattern() {
  pattern="$1"
  if ! grep -rq "$pattern" "$SRC"; then
    echo "Validation failed: expected pattern '$pattern'"
    exit 1
  fi
}

echo "Running attendance report checks…"

require_file "$SRC/domain/AttendanceRecord.kt"
require_file "$SRC/domain/Employee.kt"
require_file "$SRC/domain/AttendanceSummary.kt"
require_file "$SRC/auth/TeamAuthGuard.kt"
require_file "$SRC/data/AttendanceApi.kt"
require_file "$SRC/data/AttendanceRepository.kt"
require_file "$SRC/viewmodel/AttendanceReportViewModel.kt"
require_file "$SRC/ui/AttendanceReportScreen.kt"

require_pattern "class AttendanceReportViewModel"
require_pattern "/api/v1/attendance/summary"
require_pattern "team_id"
require_pattern "requireManagerTeamId"
require_pattern "AttendanceStatus.PRESENT"
require_pattern "AttendanceStatus.ABSENT"
require_pattern "AttendanceStatus.LEAVE"

python3 "$ROOT/tests/role_scope_test.py"
python3 "$ROOT/tests/viewmodel_test.py"
echo "All attendance report checks passed."
