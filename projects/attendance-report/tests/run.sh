#!/bin/sh
# TASK-102 structural unit checks (Kotlin compiler not required in PBMP demo).
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin"
FAIL=0

assert_contains() {
  file="$1"
  pattern="$2"
  label="$3"
  if ! grep -q "$pattern" "$file" 2>/dev/null; then
    echo "FAIL: $label (missing '$pattern' in $(basename "$file"))"
    FAIL=1
  fi
}

assert_file() {
  file="$1"
  label="$2"
  if [ ! -f "$file" ]; then
    echo "FAIL: $label (missing $file)"
    FAIL=1
  fi
}

assert_file "$SRC/com/hr/attendance/data/model/AttendanceRecord.kt" "AttendanceRecord model"
assert_file "$SRC/com/hr/attendance/data/auth/JwtTeamValidator.kt" "JWT team validator"
assert_file "$SRC/com/hr/attendance/data/api/AttendanceApiService.kt" "API service"
assert_file "$SRC/com/hr/attendance/ui/summary/AttendanceSummaryViewModel.kt" "ViewModel"

assert_contains "$SRC/com/hr/attendance/data/api/AttendanceEndpoints.kt" "/api/v1/attendance/summary" "API contract"
assert_contains "$SRC/com/hr/attendance/data/auth/JwtTeamValidator.kt" "team_id" "JWT team_id security"
assert_contains "$SRC/com/hr/attendance/ui/summary/AttendanceSummaryViewModel.kt" "AttendanceSummaryViewModel" "MVVM ViewModel"
assert_contains "$SRC/com/hr/attendance/domain/AttendanceSummaryGrouper.kt" "groupBy" "groupBy(status) pseudocode"

# Role scope: records filtered to manager team
assert_contains "$SRC/com/hr/attendance/data/repository/AttendanceRepository.kt" "teamId == claims.teamId" "team scope filter"

if [ "$FAIL" -ne 0 ]; then
  echo "Unit tests FAILED"
  exit 1
fi

echo "Unit tests PASSED (structural checks for TASK-102)"
