package com.romafrihet.server.classroom.dto

import java.time.LocalDateTime

data class ClassroomDto(
    val id: Long,
    val courseId: Long,
    val grade: Int,
    val subjectName: String,
    val classNo: Int,
    val joinCode: String?,
    val joinCodeExpiresAt: LocalDateTime?,
    val studentCount: Long,
)
