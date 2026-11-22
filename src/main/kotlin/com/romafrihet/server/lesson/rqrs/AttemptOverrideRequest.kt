package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
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
        @field:Schema(description = "문제별 분포의 answerText를 그대로 넘긴다. 공백과 대소문자는 무시하고 비교한다. 학생 답에 길이 제한이 없어 여기도 두지 않는다")
        @field:NotBlank
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
