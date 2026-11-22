package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.LessonStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class LessonDto(
    val lessonNumber: Int,
    val title: String,
    val questionCount: Long,
    val classrooms: List<LessonClassroomStatusDto>,
)

data class LessonClassroomStatusDto(
    val classroomId: Long,
    val classNo: Int,
    val status: LessonStatus,
    val scheduledOpenAt: OffsetDateTime?,
    val scheduledFinishAt: OffsetDateTime?,
)

data class ClassroomLessonStatusDto(
    val classroomId: Long,
    val courseId: Long,
    val classNo: Int,
    val currentLessonNumber: Int?,
    val latestLessonNumber: Int?,
    val isOpen: Boolean,
)

data class OpenedLessonDto(
    val lessonNumber: Int,
    val title: String,
    val openedAt: LocalDateTime,
    val status: LessonStatus,
)
