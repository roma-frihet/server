package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.dto.LessonResultDto
import com.romafrihet.server.lesson.dto.LessonResultQuestionDto
import io.swagger.v3.oas.annotations.media.Schema

class LessonResultResponse private constructor() {
    /** 마감한 차시라 정답과 해설을 함께 내린다. */
    @Schema(name = "LessonResultResponseQuestion")
    data class Question(
        val questionId: Long,
        val content: String?,
        val imageUrl: String?,
        val explanation: String?,
        val result: QuestionResult,
        val selectedOptionId: Long?,
        val answerText: String?,
        val correctOptionId: Long?,
        val correctAnswer: String?,
        val options: List<QuestionResponse.StudentOption>,
    ) {
        companion object {
            fun of(dto: LessonResultQuestionDto) = Question(
                questionId = dto.question.id,
                content = dto.question.content,
                imageUrl = dto.question.imageUrl,
                explanation = dto.question.explanation,
                result = dto.result,
                selectedOptionId = dto.selectedOptionId,
                answerText = dto.answerText,
                correctOptionId = dto.correctOptionId,
                correctAnswer = dto.correctAnswer,
                options = dto.question.options.map(QuestionResponse.StudentOption::of),
            )
        }
    }

    @Schema(name = "LessonResultResponseResult")
    data class Result(
        val lessonNumber: Int,
        @Schema(nullable = true) val correctCount: Int?,
        val questionCount: Int,
        val questions: List<Question>,
    ) {
        companion object {
            fun of(dto: LessonResultDto) = Result(
                lessonNumber = dto.lessonNumber,
                correctCount = dto.correctCount,
                questionCount = dto.questionCount,
                questions = dto.questions.map(Question::of),
            )
        }
    }
}
