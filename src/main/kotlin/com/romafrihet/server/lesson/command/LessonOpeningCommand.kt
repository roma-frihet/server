package com.romafrihet.server.lesson.command

import java.time.OffsetDateTime
import java.util.UUID

class LessonOpeningCommand private constructor() {
    data class Change(val userId: UUID, val classroomId: Long, val lessonNumber: Int)

    /**
     * 시각은 시간대를 포함해 받는다. 서버 시계는 컨테이너 시간대(UTC)를 따르므로,
     * 교사가 고른 한국 시각을 그대로 LocalDateTime으로 받으면 9시간 어긋난다.
     * 열린 차시는 openAt 없이 finishAt만 건다.
     */
    data class Schedule(
        val userId: UUID,
        val classroomId: Long,
        val lessonNumber: Int,
        val openAt: OffsetDateTime?,
        val finishAt: OffsetDateTime?,
    )
}
