package com.romafrihet.server.lesson.rqrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.dto.ClassroomLessonStatusDto
import com.romafrihet.server.lesson.dto.OpenedLessonDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class LessonOpeningResponse private constructor() {
    @Schema(name = "LessonOpeningResponseResult")
    data class Result(
        val classroomId: Long,
        val courseId: Long,
        val classNo: Int,
        val currentLessonNumber: Int?,
        val latestLessonNumber: Int?,
        @get:JsonProperty("isOpen")
        val isOpen: Boolean,
    ) {
        companion object {
            fun of(dto: ClassroomLessonStatusDto) = Result(
                classroomId = dto.classroomId,
                courseId = dto.courseId,
                classNo = dto.classNo,
                currentLessonNumber = dto.currentLessonNumber,
                latestLessonNumber = dto.latestLessonNumber,
                isOpen = dto.isOpen,
            )
        }
    }

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
