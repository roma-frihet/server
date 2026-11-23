package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.command.LessonOpeningCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

class LessonOpeningRequest private constructor() {
    @Schema(name = "LessonOpeningRequestSchedule")
    data class Schedule(
        @field:Schema(
            description = "열 시각. 시간대를 포함한다(예: 2026-11-23T09:10:00+09:00). 이미 열린 차시는 비운다",
        )
        val openAt: OffsetDateTime?,
        @field:Schema(description = "자동 마감 시각. 시간대를 포함한다. 열 시각보다 뒤여야 한다")
        val finishAt: OffsetDateTime?,
    ) {
        fun toCommand(userId: UUID, classroomId: Long, lessonNumber: Int) = LessonOpeningCommand.Schedule(
            userId = userId,
            classroomId = classroomId,
            lessonNumber = lessonNumber,
            openAt = openAt,
            finishAt = finishAt,
        )
    }
}
