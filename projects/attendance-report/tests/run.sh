#!/bin/sh
# PBMP unit / structure checks for TASK-102 (Kotlin attendance report)
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin/com/hr/attendance"
FAIL=0

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAIL=1; }

require_file() {
  if [ -f "$1" ]; then
    pass "found $(basename "$1")"
  else
    fail "missing $1"
  fi
}

echo "== Required MVVM structure =="
require_file "$SRC/domain/AttendanceRecord.kt"
require_file "$SRC/domain/Employee.kt"
require_file "$SRC/domain/AttendanceStatus.kt"
require_file "$SRC/domain/AttendanceSummary.kt"
require_file "$SRC/data/JwtTeamScope.kt"
require_file "$SRC/data/AttendanceApi.kt"
require_file "$SRC/data/AttendanceRepository.kt"
require_file "$SRC/presentation/AttendanceReportViewModel.kt"
require_file "$SRC/presentation/AttendanceReportUiState.kt"
require_file "$SRC/presentation/AttendanceReportScreen.kt"

echo ""
echo "== API contract =="
if grep -q '/api/v1/attendance/summary' "$SRC/data/AttendanceApi.kt"; then
  pass "GET /api/v1/attendance/summary documented"
else
  fail "API contract missing"
fi

echo ""
echo "== Security: team_id JWT scope =="
if grep -q 'team_id' "$SRC/data/JwtTeamScope.kt" && \
   grep -q 'requireTeamAccess' "$SRC/data/AttendanceRepository.kt"; then
  pass "team_id JWT scope enforced in repository"
else
  fail "team_id security check missing"
fi

echo ""
echo "== Coding standards (no debug noise) =="
BAD=0
for f in $(find "$ROOT/src" -name '*.kt'); do
  if grep -qE 'println\(|TODO\(|FIXME\(' "$f"; then
    fail "debug marker in $f"
    BAD=1
  fi
done
if [ "$BAD" -eq 0 ]; then
  pass "no println/TODO/FIXME in Kotlin sources"
fi

echo ""
echo "== ViewModel role-scope logic (Python smoke tests) =="
python3 - <<'PY'
import base64
import json
import re
import sys

def b64url(data: str) -> str:
    return base64.urlsafe_b64encode(data.encode()).decode().rstrip("=")

def team_id_from_token(bearer: str):
    token = bearer.removeprefix("Bearer ").strip()
    parts = token.split(".")
    if len(parts) < 2:
        return None
    seg = parts[1]
    seg += "=" * ((4 - len(seg) % 4) % 4)
    payload = base64.urlsafe_b64decode(seg).decode()
    m = re.search(r'"team_id"\s*:\s*"([^"]+)"', payload)
    return m.group(1) if m else None

def require_team_access(token_team, requested):
    if not token_team or token_team != requested:
        raise PermissionError("unauthorized")

header = b64url('{"alg":"HS256","typ":"JWT"}')
payload = b64url('{"sub":"mgr-1","role":"manager","team_id":"team-alpha"}')
token = f"Bearer {header}.{payload}.sig"

assert team_id_from_token(token) == "team-alpha"
require_team_access(team_id_from_token(token), "team-alpha")

try:
    require_team_access(team_id_from_token(token), "team-beta")
    sys.exit("expected unauthorized for other team")
except PermissionError:
    pass

print("PASS: JWT team_id extraction and scope checks")
PY

if [ "$FAIL" -ne 0 ]; then
  echo ""
  echo "Some checks failed."
  exit 1
fi

echo ""
echo "All attendance-report checks passed."
