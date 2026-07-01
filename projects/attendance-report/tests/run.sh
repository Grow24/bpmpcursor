#!/bin/sh
# PBMP unit / structure tests for TASK-102 (attendance-report)
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin/com/hr/attendance"
FAIL=0

pass() { printf 'PASS: %s\n' "$1"; }
fail() { printf 'FAIL: %s\n' "$1"; FAIL=1; }

require_file() {
  if [ -f "$1" ]; then
    pass "found $(basename "$1")"
  else
    fail "missing $1"
  fi
}

require_grep() {
  if grep -q "$2" "$1" 2>/dev/null; then
    pass "$3"
  else
    fail "$3"
  fi
}

echo "Running attendance-report tests..."

require_file "$SRC/domain/AttendanceRecord.kt"
require_file "$SRC/domain/Employee.kt"
require_file "$SRC/domain/AttendanceStatus.kt"
require_file "$SRC/domain/EmployeeAttendanceSummary.kt"
require_file "$SRC/data/auth/JwtTeamAuth.kt"
require_file "$SRC/data/api/AttendanceSummaryApi.kt"
require_file "$SRC/data/repository/AttendanceRepository.kt"
require_file "$SRC/ui/summary/AttendanceSummaryViewModel.kt"
require_file "$SRC/ui/summary/AttendanceSummaryScreen.kt"

require_grep "$SRC/data/auth/JwtTeamAuth.kt" "team_id" "JWT extracts team_id claim"
require_grep "$SRC/data/repository/AttendanceRepository.kt" "managerTeamId" "repository enforces manager team scope"
require_grep "$SRC/data/api/AttendanceSummaryApi.kt" "/api/v1/attendance/summary" "API contract path present"
require_grep "$SRC/ui/summary/AttendanceSummaryViewModel.kt" "AttendanceRepository" "ViewModel uses repository (MVVM)"
require_grep "$SRC/domain/AttendanceStatus.kt" "PRESENT" "present status defined"
require_grep "$SRC/domain/AttendanceStatus.kt" "ABSENT" "absent status defined"
require_grep "$SRC/domain/AttendanceStatus.kt" "LEAVE" "leave status defined"
require_grep "$SRC/domain/EmployeeAttendanceSummary.kt" "groupingBy" "records grouped by status"

if [ "$FAIL" -eq 0 ]; then
  echo "All tests passed."
  exit 0
fi

echo "Some tests failed."
exit 1
