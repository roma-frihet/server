package com.romafrihet.server.teacher.dto

import com.romafrihet.server.teacher.TeacherApplicationStatus
import java.time.LocalDateTime
import java.util.UUID

data class TeacherApplicationDto(
    val id: Long,
    val userId: UUID,
    val email: String,
    val schoolId: Long,
    val schoolName: String,
    val name: String,
    val status: TeacherApplicationStatus,
    val rejectReason: String?,
    val emailVerifiedAt: LocalDateTime?,
    val reviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)
