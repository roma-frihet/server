package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import io.swagger.v3.oas.annotations.media.Schema
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
}
