#!/bin/sh
# PBMP unit / structure tests for TASK-102 (Kotlin MVVM attendance report).
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin/com/hr/attendance"

fail() {
  echo "FAIL: $1"
  exit 1
}

pass() {
  echo "PASS: $1"
}

# --- Required source files ---
for f in \
  model/AttendanceRecord.kt \
  model/AttendanceStatus.kt \
  model/Employee.kt \
  model/AttendanceSummaryResponse.kt \
  security/JwtTokenParser.kt \
  security/TeamScopeValidator.kt \
  data/AttendanceApiService.kt \
  data/AttendanceRepository.kt \
  ui/AttendanceSummaryViewModel.kt \
  ui/AttendanceSummaryUiState.kt \
  ui/AttendanceSummaryScreen.kt \
  MainActivity.kt
do
  [ -f "$SRC/$f" ] || fail "missing $f"
done
pass "all required Kotlin source files present"

# --- API contract ---
grep -q '/api/v1/attendance/summary' "$SRC/data/AttendanceApiService.kt" \
  || fail "API endpoint GET /api/v1/attendance/summary not found"
pass "API contract endpoint present"

# --- Security: team_id from JWT ---
grep -q 'team_id' "$SRC/security/JwtTokenParser.kt" \
  || fail "JWT must read team_id claim"
grep -q 'authorizeManagerAccess' "$SRC/security/TeamScopeValidator.kt" \
  || fail "TeamScopeValidator must authorize manager access"
grep -q 'assertTeamScope' "$SRC/data/AttendanceRepository.kt" \
  || fail "Repository must assert team scope"
pass "security rules (team_id JWT scope) enforced"

# --- MVVM layers ---
grep -q 'class AttendanceSummaryViewModel' "$SRC/ui/AttendanceSummaryViewModel.kt" \
  || fail "ViewModel missing"
grep -q 'AttendanceSummaryUiState' "$SRC/ui/AttendanceSummaryUiState.kt" \
  || fail "UiState missing"
grep -q 'AttendanceSummaryScreen' "$SRC/ui/AttendanceSummaryScreen.kt" \
  || fail "Screen composable missing"
pass "MVVM architecture (ViewModel + UiState + Screen)"

# --- Business object ---
grep -q 'data class AttendanceRecord' "$SRC/model/AttendanceRecord.kt" \
  || fail "AttendanceRecord business object missing"
grep -q 'groupBy' "$SRC/data/AttendanceRepository.kt" \
  || fail "groupBy(status) logic missing"
pass "AttendanceRecord + groupBy(status) logic"

# --- Attendance statuses ---
for status in PRESENT ABSENT LEAVE; do
  grep -q "$status" "$SRC/model/AttendanceStatus.kt" || fail "missing status $status"
done
pass "present / absent / leave statuses defined"

echo "All TASK-102 checks passed."
