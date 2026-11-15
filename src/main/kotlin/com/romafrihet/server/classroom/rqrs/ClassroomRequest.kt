package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.command.ClassroomCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

private const val MAX_CLASS_NO = 20L

class ClassroomRequest private constructor() {
    @Schema(name = "ClassroomRequestCreate")
    data class Create(
        @field:NotNull
        val courseId: Long?,
        @field:NotNull
        @field:Min(1)
        @field:Max(MAX_CLASS_NO)
        val classNo: Int?,
    ) {
        fun toCommand(userId: UUID) = ClassroomCommand.Create(
            userId = userId,
            courseId = requireNotNull(courseId),
            classNo = requireNotNull(classNo),
        )
    }
}
