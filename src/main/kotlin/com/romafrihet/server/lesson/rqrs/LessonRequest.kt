package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.command.LessonCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

class LessonRequest private constructor() {
    @Schema(name = "LessonRequestSave")
    data class Save(
        @field:NotBlank
        @field:Size(max = 100)
        val title: String?,
    ) {
        fun toCreateCommand(userId: UUID, courseId: Long) =
            LessonCommand.Create(userId = userId, courseId = courseId, title = requireNotNull(title).trim())

        fun toRenameCommand(userId: UUID, courseId: Long, lessonNumber: Int) = LessonCommand.Rename(
            userId = userId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            title = requireNotNull(title).trim(),
        )
    }
}
