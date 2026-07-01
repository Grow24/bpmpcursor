package com.hr.attendance.security

class UnauthorizedTeamAccessException(
    message: String = "Manager can only access their own team's attendance data.",
) : SecurityException(message)
