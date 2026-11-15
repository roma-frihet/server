package com.romafrihet.server.student.rqrs

import com.romafrihet.server.student.command.StudentPasswordCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

private const val MIN_PASSWORD_LENGTH = 8

class StudentPasswordRequest private constructor() {
    @Schema(name = "StudentPasswordRequestChange")
    data class Change(
        @field:NotBlank
        @field:Size(min = MIN_PASSWORD_LENGTH, max = 72)
        val password: String?,
    ) {
        fun toCommand(userId: UUID) =
            StudentPasswordCommand.Change(userId = userId, password = requireNotNull(password))
    }
}
