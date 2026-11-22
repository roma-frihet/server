package com.romafrihet.server.report.dto

import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.report.StudentAnswerResult

data class StudentLessonAnswersDto(
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val lessonNumber: Int,
    val lessonTitle: String,
    val status: LessonStatus,
    val questionCount: Int,
    val correctCount: Int,
    val answers: List<StudentLessonAnswerDto>,
)

data class StudentLessonAnswerDto(
    val questionId: Long,
    val displayOrder: Int,
    val type: QuestionType,
    val content: String?,
    val hasImage: Boolean,
    val explanation: String?,
    val options: List<AnswerOptionDto>,
    val acceptedAnswers: List<String>,
    val selectedOptionId: Long?,
    val answerText: String?,
    val result: StudentAnswerResult,
    val attemptAnswerId: Long?,
    val isOverridden: Boolean,
    val byTimeout: Boolean,
)

data class AnswerOptionDto(
    val optionId: Long,
    val displayOrder: Int,
    val content: String?,
    val hasImage: Boolean,
    val isCorrect: Boolean,
)
