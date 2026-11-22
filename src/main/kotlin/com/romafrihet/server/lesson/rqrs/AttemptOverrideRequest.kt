package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

class AttemptOverrideRequest private constructor() {
    @Schema(name = "AttemptOverrideRequestUpdate")
    data class Update(
        @field:NotNull
        val result: QuestionResult?,
    ) {
        fun toCommand(userId: UUID, attemptAnswerId: Long) = AttemptOverrideCommand.Update(
            userId = userId,
            attemptAnswerId = attemptAnswerId,
            result = requireNotNull(result),
        )
    }

    @Schema(name = "AttemptOverrideRequestByAnswer")
    data class ByAnswer(
        @field:Schema(description = "문제별 분포의 answerText를 그대로 넘긴다. 공백과 대소문자는 무시하고 비교한다")
        @field:NotBlank
        @field:Size(max = MAX_ANSWER_LENGTH)
        val answerText: String?,
        @field:Schema(description = "인정은 RIGHT, 인정 취소는 WRONG")
        @field:NotNull
        val result: QuestionResult?,
    ) {
        fun toCommand(userId: UUID, classroomId: Long, lessonNumber: Int, questionId: Long) =
            AttemptOverrideCommand.ByAnswer(
                userId = userId,
                classroomId = classroomId,
                lessonNumber = lessonNumber,
                questionId = questionId,
                answerText = requireNotNull(answerText),
                result = requireNotNull(result),
            )
    }
}

private const val MAX_ANSWER_LENGTH = 500
