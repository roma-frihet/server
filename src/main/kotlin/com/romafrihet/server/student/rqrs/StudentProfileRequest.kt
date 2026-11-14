package com.romafrihet.server.student.rqrs

import com.romafrihet.server.student.command.StudentProfileCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

private const val MAX_GRADE = 3L
private const val MAX_CLASS_NUMBER = 99L
private const val MAX_SEAT_NUMBER = 99L

class StudentProfileRequest private constructor() {
    @Schema(name = "StudentProfileRequestCreate")
    data class Create(
        @field:NotNull
        val schoolId: Long?,
        @field:NotNull
        @field:Min(1)
        @field:Max(MAX_GRADE)
        val grade: Int?,
        @field:NotNull
        @field:Min(1)
        @field:Max(MAX_CLASS_NUMBER)
        val classNumber: Int?,
        @field:NotNull
        @field:Min(1)
        @field:Max(MAX_SEAT_NUMBER)
        val seatNumber: Int?,
        @field:NotBlank
        @field:Size(max = 50)
        val name: String?,
    ) {
        fun toCommand(userId: UUID) = StudentProfileCommand.Create(
            userId = userId,
            schoolId = requireNotNull(schoolId),
            grade = requireNotNull(grade),
            classNumber = requireNotNull(classNumber),
            seatNumber = requireNotNull(seatNumber),
            name = requireNotNull(name).trim(),
        )
    }
}
