package com.romafrihet.server.student.projection

import java.time.LocalDateTime
import java.util.UUID

data class StudentProfileSummaryProjection(
    val userId: UUID,
    val schoolId: Long,
    val schoolName: String,
    val grade: Int,
    val classNumber: Int,
    val seatNumber: Int,
    val name: String,
    val mustChangePassword: Boolean,
    val createdAt: LocalDateTime,
)
