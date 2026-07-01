package hr.attendance.model

data class AttendanceSummary(
    val month: String,
    val teamId: String,
    val present: Int,
    val absent: Int,
    val leave: Int,
) {
    val totalDays: Int get() = present + absent + leave
}
