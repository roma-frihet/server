package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.dto.ClassroomDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class ClassroomResponse private constructor() {
    @Schema(name = "ClassroomResponseResult")
    data class Result(
        val id: Long,
        val courseId: Long,
        val name: String,
        val joinCode: String?,
        val joinCodeExpiresAt: LocalDateTime?,
        val studentCount: Long,
    ) {
        companion object {
            fun of(dto: ClassroomDto) = Result(
                id = dto.id,
                courseId = dto.courseId,
                name = dto.name,
                joinCode = dto.joinCode,
                joinCodeExpiresAt = dto.joinCodeExpiresAt,
                studentCount = dto.studentCount,
            )
        }
    }
}
