package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.QuestionResult

/** 마감한 차시의 결과다. 응시하지 않았으면 correctCount가 null이다. */
data class LessonResultDto(
    val lessonNumber: Int,
    val correctCount: Int?,
    val questionCount: Int,
    val questions: List<LessonResultQuestionDto>,
)

data class LessonResultQuestionDto(
    val question: QuestionDto,
    val result: QuestionResult,
    val selectedOptionId: Long?,
    val answerText: String?,
    val correctOptionId: Long?,
    val correctAnswer: String?,
)
