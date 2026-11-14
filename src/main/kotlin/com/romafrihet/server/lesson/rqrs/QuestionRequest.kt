package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.QuestionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

private const val MIN_TIME_LIMIT_SEC = 1L
private const val MAX_TIME_LIMIT_SEC = 300L
private const val DEFAULT_TIME_LIMIT_SEC = 30

class QuestionRequest private constructor() {
    @Schema(name = "QuestionRequestOption")
    data class Option(
        val content: String?,
        val imageId: Long?,
        @field:NotNull
        val isCorrect: Boolean?,
    ) {
        fun toCommand() = QuestionCommand.Option(
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            isCorrect = requireNotNull(isCorrect),
        )
    }

    @Schema(name = "QuestionRequestSave")
    data class Save(
        @field:NotNull
        val type: QuestionType?,
        val content: String?,
        val imageId: Long?,
        @field:Min(MIN_TIME_LIMIT_SEC)
        @field:Max(MAX_TIME_LIMIT_SEC)
        val timeLimitSec: Int? = null,
        val explanation: String? = null,
        @field:NotEmpty
        @field:Valid
        val options: List<Option>?,
    ) {
        /** 값을 주지 않으면 30초로 둔다. */
        private fun timeLimit() = timeLimitSec ?: DEFAULT_TIME_LIMIT_SEC

        /** 빈 해설은 없는 것으로 저장한다. */
        private fun explanationOrNull() = explanation?.trim()?.ifEmpty { null }

        fun toCreateCommand(userId: UUID, courseId: Long, lessonNumber: Int) = QuestionCommand.Create(
            userId = userId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            timeLimitSec = timeLimit(),
            explanation = explanationOrNull(),
            options = requireNotNull(options).map(Option::toCommand),
        )

        fun toUpdateCommand(userId: UUID, questionId: Long) = QuestionCommand.Update(
            userId = userId,
            questionId = questionId,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            timeLimitSec = timeLimit(),
            explanation = explanationOrNull(),
            options = requireNotNull(options).map(Option::toCommand),
        )
    }
}
