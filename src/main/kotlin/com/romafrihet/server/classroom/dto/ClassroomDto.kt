package com.romafrihet.server.classroom.dto

import java.time.LocalDateTime

data class ClassroomDto(
    val id: Long,
    val courseId: Long,
    val name: String,
    val joinCode: String?,
    val joinCodeExpiresAt: LocalDateTime?,
    val studentCount: Long,
)
