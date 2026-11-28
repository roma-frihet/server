package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.QuestionType
import java.util.UUID

data class QuestionAdminQueryDto(
    val schoolId: Long?,
    val subjectId: Long?,
    val teacherUserId: UUID?,
    val type: QuestionType?,
    val keyword: String?,
)
