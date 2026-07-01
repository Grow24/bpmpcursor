#!/usr/bin/env python3
"""ViewModel unit tests for TASK-102 attendance report (Python mirror of MVVM flow)."""

from __future__ import annotations

import base64
import json
import sys
from dataclasses import dataclass
from enum import Enum
from typing import List, Tuple


class SecurityError(Exception):
    pass


class AttendanceStatus(Enum):
    PRESENT = "PRESENT"
    ABSENT = "ABSENT"
    LEAVE = "LEAVE"


@dataclass
class Employee:
    id: str
    name: str
    team_id: str


@dataclass
class AttendanceRecord:
    employee_id: str
    date: str
    status: AttendanceStatus


@dataclass
class AttendanceSummary:
    year: int
    month: int
    team_id: str
    total_present: int
    total_absent: int
    total_leave: int
    employee_count: int


def make_token(team_id: str, role: str) -> str:
    header = base64.urlsafe_b64encode(
        json.dumps({"alg": "none", "typ": "JWT"}).encode()
    ).decode().rstrip("=")
    payload = base64.urlsafe_b64encode(
        json.dumps({"team_id": team_id, "role": role, "sub": "mgr-1"}).encode()
    ).decode().rstrip("=")
    return f"{header}.{payload}.signature"


def decode_jwt(token: str) -> dict:
    parts = token.split(".")
    padded = parts[1] + "=" * (-len(parts[1]) % 4)
    return json.loads(base64.urlsafe_b64decode(padded.encode()))


def require_manager_team_id(token: str) -> str:
    claims = decode_jwt(token)
    if claims.get("role", "").lower() != "manager":
        raise SecurityError("Only managers can view team attendance")
    team_id = claims.get("team_id", "")
    if not team_id:
        raise SecurityError("Missing team_id in JWT token")
    return str(team_id)


def build_summary(
    team_id: str,
    year: int,
    month: int,
    employees: List[Employee],
    records: List[AttendanceRecord],
) -> AttendanceSummary:
    grouped = {}
    for record in records:
        grouped.setdefault(record.employee_id, []).append(record)

    total_present = total_absent = total_leave = 0
    for employee in employees:
        for record in grouped.get(employee.id, []):
            if record.status == AttendanceStatus.PRESENT:
                total_present += 1
            elif record.status == AttendanceStatus.ABSENT:
                total_absent += 1
            elif record.status == AttendanceStatus.LEAVE:
                total_leave += 1

    return AttendanceSummary(
        year=year,
        month=month,
        team_id=team_id,
        total_present=total_present,
        total_absent=total_absent,
        total_leave=total_leave,
        employee_count=len(employees),
    )


def load_monthly_report(
    token: str,
    year: int,
    month: int,
    employees: List[Employee],
    records: List[AttendanceRecord],
) -> Tuple[str, AttendanceSummary | None]:
    try:
        team_id = require_manager_team_id(token)
        team_employees = [e for e in employees if e.team_id == team_id]
        employee_ids = {e.id for e in team_employees}
        month_records = [
            r for r in records
            if r.employee_id in employee_ids
        ]
        summary = build_summary(team_id, year, month, team_employees, month_records)
        return "success", summary
    except SecurityError as exc:
        return "error", str(exc)


def test_viewmodel_loads_summary_for_manager() -> None:
    token = make_token("team-alpha", "manager")
    employees = [
        Employee("e1", "Alice", "team-alpha"),
        Employee("e2", "Bob", "team-alpha"),
    ]
    records = [
        AttendanceRecord("e1", "2025-06-01", AttendanceStatus.PRESENT),
        AttendanceRecord("e1", "2025-06-02", AttendanceStatus.ABSENT),
        AttendanceRecord("e2", "2025-06-01", AttendanceStatus.LEAVE),
    ]
    state, result = load_monthly_report(token, 2025, 6, employees, records)
    assert state == "success"
    assert result is not None
    assert result.team_id == "team-alpha"
    assert result.total_present == 1
    assert result.total_absent == 1
    assert result.total_leave == 1
    assert result.employee_count == 2


def test_viewmodel_rejects_non_manager() -> None:
    token = make_token("team-alpha", "employee")
    state, message = load_monthly_report(token, 2025, 6, [], [])
    assert state == "error"
    assert "manager" in str(message).lower()


def test_viewmodel_excludes_other_teams() -> None:
    token = make_token("team-alpha", "manager")
    employees = [
        Employee("e1", "Alice", "team-alpha"),
        Employee("e3", "Carol", "team-beta"),
    ]
    records = [
        AttendanceRecord("e1", "2025-06-01", AttendanceStatus.PRESENT),
        AttendanceRecord("e3", "2025-06-01", AttendanceStatus.PRESENT),
    ]
    state, result = load_monthly_report(token, 2025, 6, employees, records)
    assert state == "success"
    assert result is not None
    assert result.employee_count == 1
    assert result.total_present == 1


def main() -> int:
    test_viewmodel_loads_summary_for_manager()
    test_viewmodel_rejects_non_manager()
    test_viewmodel_excludes_other_teams()
    print("viewmodel unit tests passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
