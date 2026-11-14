package com.romafrihet.server.student.dto

data class StudentProfileDto(
    val schoolId: Long,
    val schoolName: String,
    val grade: Int,
    val classNumber: Int,
    val seatNumber: Int,
    val studentNumber: String,
    val name: String,
)
