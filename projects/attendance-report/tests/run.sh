#!/bin/sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/app/src"
FAIL=0

pass() {
  printf 'PASS: %s\n' "$1"
}

fail() {
  printf 'FAIL: %s\n' "$1"
  FAIL=1
}

require_file() {
  if [ -f "$1" ]; then
    pass "found $(basename "$1")"
  else
    fail "missing $1"
  fi
}

require_pattern() {
  if grep -q "$2" "$1"; then
    pass "$3"
  else
    fail "$3"
  fi
}

require_file "$APP/main/kotlin/com/hr/attendance/domain/Models.kt"
require_file "$APP/main/kotlin/com/hr/attendance/data/api/AttendanceApiService.kt"
require_file "$APP/main/kotlin/com/hr/attendance/data/auth/JwtTokenProvider.kt"
require_file "$APP/main/kotlin/com/hr/attendance/data/repository/AttendanceRepository.kt"
require_file "$APP/main/kotlin/com/hr/attendance/presentation/AttendanceSummaryViewModel.kt"
require_file "$APP/main/kotlin/com/hr/attendance/presentation/AttendanceSummaryScreen.kt"
require_file "$APP/test/kotlin/com/hr/attendance/presentation/AttendanceSummaryViewModelTest.kt"
require_file "$APP/test/kotlin/com/hr/attendance/data/repository/AttendanceRepositoryTest.kt"

require_pattern "$APP/main/kotlin/com/hr/attendance/data/api/AttendanceApiService.kt" \
  '/api/v1/attendance/summary' \
  'API contract GET /api/v1/attendance/summary'

require_pattern "$APP/main/kotlin/com/hr/attendance/data/auth/JwtTokenProvider.kt" \
  'team_id' \
  'JWT team_id extraction'

require_pattern "$APP/main/kotlin/com/hr/attendance/data/repository/AttendanceRepository.kt" \
  'UnauthorizedTeamAccessException' \
  'team scope enforcement'

require_pattern "$APP/test/kotlin/com/hr/attendance/data/repository/AttendanceRepositoryTest.kt" \
  'rejectsMismatchedTeam' \
  'role scope unit test'

require_pattern "$APP/test/kotlin/com/hr/attendance/presentation/AttendanceSummaryViewModelTest.kt" \
  'loadMonthlySummary_rendersGroupedCounts' \
  'ViewModel unit test'

if [ "$FAIL" -ne 0 ]; then
  exit 1
fi

echo 'All attendance-report checks passed.'
