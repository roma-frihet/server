package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.QuestionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

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
        @field:NotEmpty
        @field:Valid
        val options: List<Option>?,
    ) {
        fun toCreateCommand(userId: UUID, courseId: Long, lessonNumber: Int) = QuestionCommand.Create(
            userId = userId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            options = requireNotNull(options).map(Option::toCommand),
        )

        fun toUpdateCommand(userId: UUID, questionId: Long) = QuestionCommand.Update(
            userId = userId,
            questionId = questionId,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            options = requireNotNull(options).map(Option::toCommand),
        )
    }
}
