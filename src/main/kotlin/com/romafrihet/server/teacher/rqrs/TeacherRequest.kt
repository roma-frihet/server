package com.romafrihet.server.teacher.rqrs

import com.romafrihet.server.teacher.command.TeacherCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

class TeacherRequest private constructor() {
    @Schema(name = "TeacherRequestSaveProfile")
    data class SaveProfile(
        @field:NotNull
        val schoolId: Long?,
        @field:NotBlank
        @field:Size(max = 50)
        val name: String?,
    ) {
        fun toCommand(userId: UUID) = TeacherCommand.SaveProfile(
            userId = userId,
            schoolId = requireNotNull(schoolId),
            name = requireNotNull(name).trim(),
        )
    }
}
