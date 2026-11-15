package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.dto.AttemptDto
import com.romafrihet.server.lesson.dto.CurrentQuestionDto
import com.romafrihet.server.lesson.dto.FinalizedAnswerDto
import io.swagger.v3.oas.annotations.media.Schema

class AttemptResponse private constructor() {
    /** 푸는 중인 문제다. 정답 여부는 담지 않는다. */
    @Schema(name = "AttemptResponseCurrent")
    data class Current(
        val questionIndex: Int,
        val question: QuestionResponse.StudentResult,
        val remainingMillis: Long,
        val draftOptionId: Long?,
        val draftText: String?,
    ) {
        companion object {
            fun of(dto: CurrentQuestionDto) = Current(
                questionIndex = dto.questionIndex,
                question = QuestionResponse.StudentResult.of(dto.question),
                remainingMillis = dto.remainingMillis,
                draftOptionId = dto.draftOptionId,
                draftText = dto.draftText,
            )
        }
    }

    /** 직전에 확정된 문제다. 정답과 해설은 여기에만 담긴다. */
    @Schema(name = "AttemptResponseLast")
    data class Last(
        val questionIndex: Int,
        val result: QuestionResult,
        val byTimeout: Boolean,
        val selectedOptionId: Long?,
        val answerText: String?,
        val correctOptionId: Long?,
        val correctAnswer: String?,
        val explanation: String?,
    ) {
        companion object {
            fun of(dto: FinalizedAnswerDto) = Last(
                questionIndex = dto.questionIndex,
                result = dto.result,
                byTimeout = dto.byTimeout,
                selectedOptionId = dto.selectedOptionId,
                answerText = dto.answerText,
                correctOptionId = dto.correctOptionId,
                correctAnswer = dto.correctAnswer,
                explanation = dto.explanation,
            )
        }
    }

    @Schema(name = "AttemptResponseResult")
    data class Result(
        val phase: AttemptPhase,
        val total: Int,
        val nextIndex: Int,
        @Schema(nullable = true) val nextTimeLimitSec: Int?,
        @Schema(nullable = true) val correctCount: Int?,
        @Schema(nullable = true) val current: Current?,
        @Schema(nullable = true) val last: Last?,
    ) {
        companion object {
            fun of(dto: AttemptDto) = Result(
                phase = dto.phase,
                total = dto.total,
                nextIndex = dto.nextIndex,
                nextTimeLimitSec = dto.nextTimeLimitSec,
                correctCount = dto.correctCount,
                current = dto.current?.let(Current::of),
                last = dto.last?.let(Last::of),
            )
        }
    }
}
