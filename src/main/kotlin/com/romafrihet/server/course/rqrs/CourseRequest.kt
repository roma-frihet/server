package com.romafrihet.server.course.rqrs

import com.romafrihet.server.course.command.CourseCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

private const val MAX_GRADE = 3L

class CourseRequest private constructor() {
    @Schema(name = "CourseRequestCreate")
    data class Create(
        @field:NotNull
        val schoolId: Long?,
        @field:NotNull
        @field:Min(1)
        @field:Max(MAX_GRADE)
        val grade: Int?,
        @field:NotNull
        val subjectId: Long?,
    ) {
        fun toCommand(userId: UUID) = CourseCommand.Create(
            userId = userId,
            schoolId = requireNotNull(schoolId),
            grade = requireNotNull(grade),
            subjectId = requireNotNull(subjectId),
        )
    }
}
