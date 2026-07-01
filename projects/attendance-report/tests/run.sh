#!/bin/sh
# PBMP unit/structure tests for TASK-102 (Kotlin MVVM attendance report)
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin/com/hr/attendance"
FAIL=0

pass() { printf 'PASS: %s\n' "$1"; }
fail() { printf 'FAIL: %s\n' "$1"; FAIL=1; }

require_file() {
  if [ -f "$1" ]; then
    pass "file exists: $(basename "$1")"
  else
    fail "missing file: $1"
  fi
}

require_pattern() {
  file="$1"
  pattern="$2"
  label="$3"
  if grep -q "$pattern" "$file" 2>/dev/null; then
    pass "$label"
  else
    fail "$label (pattern not found in $(basename "$file"))"
  fi
}

echo "=== TASK-102 Attendance Report tests ==="

# MVVM layer files
require_file "$SRC/domain/model/AttendanceRecord.kt"
require_file "$SRC/domain/model/Employee.kt"
require_file "$SRC/domain/model/AttendanceStatus.kt"
require_file "$SRC/domain/repository/AttendanceRepository.kt"
require_file "$SRC/data/auth/JwtTeamValidator.kt"
require_file "$SRC/data/remote/AttendanceApiService.kt"
require_file "$SRC/data/repository/AttendanceRepositoryImpl.kt"
require_file "$SRC/presentation/summary/AttendanceSummaryViewModel.kt"
require_file "$SRC/presentation/summary/AttendanceSummaryScreen.kt"

# API contract
require_pattern "$SRC/data/remote/AttendanceApiService.kt" \
  '/api/v1/attendance/summary' \
  'API endpoint GET /api/v1/attendance/summary'

# Security: team_id in JWT
require_pattern "$SRC/data/auth/JwtTeamValidator.kt" \
  'team_id' \
  'JWT team_id validation'
require_pattern "$SRC/data/repository/AttendanceRepositoryImpl.kt" \
  'requireTeamAccess' \
  'Repository enforces team access before fetch'

# Attendance statuses
require_pattern "$SRC/domain/model/AttendanceStatus.kt" 'PRESENT' 'PRESENT status defined'
require_pattern "$SRC/domain/model/AttendanceStatus.kt" 'ABSENT' 'ABSENT status defined'
require_pattern "$SRC/domain/model/AttendanceStatus.kt" 'LEAVE' 'LEAVE status defined'

# groupBy(status) flow
require_pattern "$SRC/data/repository/AttendanceRepositoryImpl.kt" \
  'groupByStatus' \
  'Records grouped by status'
require_pattern "$SRC/presentation/summary/AttendanceSummaryViewModel.kt" \
  'fetchMonthlySummary' \
  'ViewModel fetches monthly summary'

# No hardcoded secrets in Kotlin sources
if grep -rEi 'password\s*=\s*["'"'"'][^"'"'"']+["'"'"']|api[_-]?key\s*=\s*["'"'"'][^"'"'"']+["'"'"']|secret\s*=\s*["'"'"'][^"'"'"']+["'"'"']' \
  "$ROOT/src" >/dev/null 2>&1; then
  fail 'hardcoded secret pattern found in source'
else
  pass 'no hardcoded secrets in Kotlin sources'
fi

if [ "$FAIL" -ne 0 ]; then
  echo "=== Tests FAILED ==="
  exit 1
fi

echo "=== All tests PASSED ==="
exit 0
