package hr.attendance.security

data class JwtClaims(
    val subject: String,
    val teamId: String,
    val role: String,
)
