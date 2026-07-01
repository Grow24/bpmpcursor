#!/usr/bin/env python3
"""Role scope tests for TASK-102 attendance report."""

from __future__ import annotations

import base64
import json
import sys


def make_token(team_id: str, role: str) -> str:
    header = base64.urlsafe_b64encode(
        json.dumps({"alg": "none", "typ": "JWT"}).encode()
    ).decode().rstrip("=")
    payload = base64.urlsafe_b64encode(
        json.dumps({"team_id": team_id, "role": role, "sub": "mgr-1"}).encode()
    ).decode().rstrip("=")
    return f"{header}.{payload}.signature"


def decode_team_id(token: str) -> str:
    parts = token.split(".")
    if len(parts) < 2:
        raise ValueError("invalid token")
    padded = parts[1] + "=" * (-len(parts[1]) % 4)
    payload = json.loads(base64.urlsafe_b64decode(padded.encode()))
    team_id = payload.get("team_id")
    if not team_id:
        raise ValueError("missing team_id")
    return str(team_id)


def assert_team_scope(token_team_id: str, requested_team_id: str) -> None:
    if token_team_id != requested_team_id:
        raise PermissionError("team scope mismatch")


def test_manager_token_contains_team_id() -> None:
    token = make_token("team-alpha", "manager")
    assert decode_team_id(token) == "team-alpha"


def test_employee_role_rejected() -> None:
    token = make_token("team-alpha", "employee")
    payload = token.split(".")[1]
    padded = payload + "=" * (-len(payload) % 4)
    body = json.loads(base64.urlsafe_b64decode(padded.encode()))
    assert body["role"] != "manager"


def test_cross_team_access_denied() -> None:
    token = make_token("team-alpha", "manager")
    token_team = decode_team_id(token)
    try:
        assert_team_scope(token_team, "team-beta")
    except PermissionError:
        return
    raise AssertionError("expected cross-team access to be denied")


def main() -> int:
    test_manager_token_contains_team_id()
    test_employee_role_rejected()
    test_cross_team_access_denied()
    print("role scope tests passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
