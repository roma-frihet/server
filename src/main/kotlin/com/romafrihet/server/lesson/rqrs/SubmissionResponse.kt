package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.dto.SubmissionQuestionDto
import com.romafrihet.server.lesson.dto.SubmissionResultDto
import io.swagger.v3.oas.annotations.media.Schema

class SubmissionResponse private constructor() {
    @Schema(name = "SubmissionResponseQuestion")
    data class Question(
        val question: QuestionResponse.Result,
        val selectedOptionId: Long?,
        val answerText: String?,
        val isCorrect: Boolean,
    ) {
        companion object {
            fun of(dto: SubmissionQuestionDto) = Question(
                question = QuestionResponse.Result.of(dto.question),
                selectedOptionId = dto.selectedOptionId,
                answerText = dto.answerText,
                isCorrect = dto.isCorrect,
            )
        }
    }

    @Schema(name = "SubmissionResponseResult")
    data class Result(
        val lessonNumber: Int,
        val correctCount: Int?,
        val questionCount: Int,
        val questions: List<Question>,
    ) {
        companion object {
            fun of(dto: SubmissionResultDto) = Result(
                lessonNumber = dto.lessonNumber,
                correctCount = dto.correctCount,
                questionCount = dto.questionCount,
                questions = dto.questions.map(Question::of),
            )
        }
    }
}
