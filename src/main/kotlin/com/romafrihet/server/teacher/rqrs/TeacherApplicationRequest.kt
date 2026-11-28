package com.romafrihet.server.teacher.rqrs

import com.romafrihet.server.teacher.command.TeacherApplicationCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

class TeacherApplicationRequest private constructor() {
    @Schema(name = "TeacherApplicationRequestApply")
    data class Apply(
        @field:NotNull
        val schoolId: Long?,
        @field:NotBlank
        @field:Size(max = 50)
        val name: String?,
    ) {
        fun toCommand(userId: UUID, email: String?) = TeacherApplicationCommand.Apply(
            userId = userId,
            email = email,
            schoolId = requireNotNull(schoolId),
            name = requireNotNull(name).trim(),
        )
    }
}
