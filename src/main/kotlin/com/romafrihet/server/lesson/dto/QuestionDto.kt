package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.QuestionType

data class QuestionDto(
    val id: Long,
    val lessonNumber: Int,
    val type: QuestionType,
    val content: String?,
    val imageId: Long?,
    val imageUrl: String?,
    val options: List<QuestionOptionDto>,
)

data class QuestionOptionDto(
    val id: Long,
    val displayOrder: Int,
    val content: String?,
    val imageId: Long?,
    val imageUrl: String?,
    val isCorrect: Boolean,
)
