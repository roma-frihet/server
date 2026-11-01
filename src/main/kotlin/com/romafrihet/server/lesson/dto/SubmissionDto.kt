package com.romafrihet.server.lesson.dto

data class SubmissionResultDto(
    val lessonNumber: Int,
    val correctCount: Int?,
    val questionCount: Int,
    val questions: List<SubmissionQuestionDto>,
)

data class SubmissionQuestionDto(
    val question: QuestionDto,
    val selectedOptionId: Long?,
    val answerText: String?,
    val isCorrect: Boolean,
)
