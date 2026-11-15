package com.romafrihet.server.student.rqrs

import com.romafrihet.server.student.command.StudentPasswordCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

// Supabase Auth의 minimum_password_length와 같아야 한다. 여기만 낮추면 서버는 받아 주는데
// Supabase가 거부해서 비밀번호가 안 바뀐다. 쓰는 사람이 중학생이라 6자로 맞춰 두었다.
private const val MIN_PASSWORD_LENGTH = 6

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
