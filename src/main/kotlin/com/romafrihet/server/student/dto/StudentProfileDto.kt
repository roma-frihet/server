package com.romafrihet.server.student.dto

data class StudentProfileDto(
    val schoolId: Long,
    val schoolName: String,
    val grade: Int,
    val classNumber: Int,
    val seatNumber: Int,
    val studentNumber: String,
    val name: String,
    /** 교사가 초기화한 임시 비밀번호 상태다. 프론트가 비밀번호 변경 화면으로 보낸다. */
    val mustChangePassword: Boolean,
)
