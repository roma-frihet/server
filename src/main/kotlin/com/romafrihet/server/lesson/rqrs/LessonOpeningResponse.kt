package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.dto.OpenedLessonDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class LessonOpeningResponse private constructor() {
    @Schema(name = "LessonOpeningResponseOpenedResult")
    data class OpenedResult(
        val lessonNumber: Int,
        val title: String,
        val openedAt: LocalDateTime,
        val status: LessonStatus,
    ) {
        companion object {
            fun of(dto: OpenedLessonDto) = OpenedResult(
                lessonNumber = dto.lessonNumber,
                title = dto.title,
                openedAt = dto.openedAt,
                status = dto.status,
            )
        }
    }
}
