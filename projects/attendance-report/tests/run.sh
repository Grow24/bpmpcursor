#!/bin/sh
# Unit tests for TASK-102 attendance report (ViewModel + role scope).
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/main/kotlin"
TESTS="$ROOT/tests/kotlin"
OUT="$ROOT/build/test-classes"
KOTLINC="${KOTLINC:-/tmp/kotlinc/bin/kotlinc}"
KOTLIN="${KOTLIN:-/tmp/kotlinc/bin/kotlin}"

if [ ! -x "$KOTLINC" ]; then
  echo "Kotlin compiler not found at $KOTLINC"
  echo "Install kotlinc or set KOTLINC/KOTLIN env vars."
  exit 1
fi

mkdir -p "$OUT"

echo "Compiling sources..."
"$KOTLINC" -d "$OUT" $(find "$SRC" -name '*.kt' | sort)

echo "Compiling and running tests..."
"$KOTLINC" -cp "$OUT" -include-runtime -d "$OUT/tests.jar" \
  "$TESTS/TeamScopeGuardTest.kt" \
  "$TESTS/AttendanceSummaryViewModelTest.kt"

"$KOTLIN" -classpath "$OUT:$OUT/tests.jar" com.hr.attendance.tests.TeamScopeGuardTestKt
"$KOTLIN" -classpath "$OUT:$OUT/tests.jar" com.hr.attendance.tests.AttendanceSummaryViewModelTestKt

echo "Unit tests passed."
