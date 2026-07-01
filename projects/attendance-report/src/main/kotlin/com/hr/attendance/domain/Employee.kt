package com.hr.attendance.domain

/**
 * Employee linked to one or more [AttendanceRecord] entries.
 */
data class Employee(
    val id: String,
    val name: String,
    val teamId: String,
)
