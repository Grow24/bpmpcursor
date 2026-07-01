package com.hr.attendance.domain

/**
 * Monthly attendance totals for one employee, grouped by status.
 */
data class EmployeeAttendanceSummary(
    val employee: Employee,
    val presentDays: Int,
    val absentDays: Int,
    val leaveDays: Int,
    val records: List<AttendanceRecord>,
) {
    companion object {
        fun fromRecords(employee: Employee, records: List<AttendanceRecord>): EmployeeAttendanceSummary {
            val grouped = records.groupingBy { it.status }.eachCount()
            return EmployeeAttendanceSummary(
                employee = employee,
                presentDays = grouped[AttendanceStatus.PRESENT] ?: 0,
                absentDays = grouped[AttendanceStatus.ABSENT] ?: 0,
                leaveDays = grouped[AttendanceStatus.LEAVE] ?: 0,
                records = records.sortedBy { it.date },
            )
        }
    }
}
