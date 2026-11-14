package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.command.ClassroomCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

class ClassroomRequest private constructor() {
    @Schema(name = "ClassroomRequestCreate")
    data class Create(
        @field:NotNull
        val courseId: Long?,
        @field:NotBlank
        @field:Size(max = 50)
        val name: String?,
    ) {
        fun toCommand(userId: UUID) = ClassroomCommand.Create(
            userId = userId,
            courseId = requireNotNull(courseId),
            name = requireNotNull(name).trim(),
        )
    }
}
