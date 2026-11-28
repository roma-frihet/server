package com.romafrihet.server.teacher.projection

import java.time.LocalDateTime
import java.util.UUID

data class TeacherProfileSummaryProjection(
    val userId: UUID,
    val name: String,
    val schoolId: Long,
    val schoolName: String,
    val createdAt: LocalDateTime,
)
