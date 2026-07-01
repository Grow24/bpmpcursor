package com.hr.attendance.data.auth

class TeamAccessDeniedException(
    message: String = "Manager may only view their own team's attendance data.",
) : SecurityException(message)

data class ManagerSession(
    val managerId: String,
    val teamId: String,
)

interface TeamAuth {
    fun authenticate(jwtToken: String): ManagerSession
}
