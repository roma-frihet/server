package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.command.SubmissionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.util.UUID

class SubmissionRequest private constructor() {
    @Schema(name = "SubmissionRequestAnswer")
    data class Answer(
        @field:NotNull
        val questionId: Long?,
        val optionId: Long?,
        val text: String?,
    ) {
        fun toCommand() = SubmissionCommand.Answer(
            questionId = requireNotNull(questionId),
            optionId = optionId,
            text = text?.trim()?.ifEmpty { null },
        )
    }

    @Schema(name = "SubmissionRequestCreate")
    data class Create(
        @field:NotNull
        @field:Valid
        val answers: List<Answer>?,
    ) {
        fun toCommand(userId: UUID, classroomId: Long, lessonNumber: Int) = SubmissionCommand.Create(
            userId = userId,
            classroomId = classroomId,
            lessonNumber = lessonNumber,
            answers = requireNotNull(answers).map(Answer::toCommand),
        )
    }
}
