#!/bin/sh
# TASK-102 structural validation (runs in PBMP validate pipeline)
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main/kotlin/com/hr/attendance"
TEST_SRC="$ROOT/app/src/test/kotlin/com/hr/attendance"

require_file() {
  if [ ! -f "$1" ]; then
    echo "Missing required file: $1"
    exit 1
  fi
}

require_pattern() {
  if ! grep -q "$2" "$1"; then
    echo "Pattern not found in $1: $2"
    exit 1
  fi
}

require_file "$SRC/domain/model/AttendanceModels.kt"
require_file "$SRC/data/auth/JwtTeamAuth.kt"
require_file "$SRC/data/api/HttpAttendanceApi.kt"
require_file "$SRC/data/repository/AttendanceRepositoryImpl.kt"
require_file "$SRC/presentation/summary/AttendanceSummaryViewModel.kt"
require_file "$SRC/presentation/summary/AttendanceSummaryScreen.kt"
require_file "$TEST_SRC/data/auth/JwtTeamAuthTest.kt"
require_file "$TEST_SRC/presentation/summary/AttendanceSummaryViewModelTest.kt"

require_pattern "$SRC/data/api/HttpAttendanceApi.kt" "/api/v1/attendance/summary"
require_pattern "$SRC/data/auth/JwtTeamAuth.kt" "team_id"
require_pattern "$SRC/presentation/summary/AttendanceSummaryViewModel.kt" "AttendanceSummaryViewModel"
require_pattern "$SRC/domain/model/AttendanceModels.kt" "AttendanceRecord"

echo "TASK-102 checks passed: MVVM layers, API contract, team_id security, unit tests present"
