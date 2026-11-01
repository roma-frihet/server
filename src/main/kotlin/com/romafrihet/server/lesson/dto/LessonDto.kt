package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.LessonStatus

data class LessonDto(
    val lessonNumber: Int,
    val title: String,
    val questionCount: Long,
    val classrooms: List<LessonClassroomStatusDto>,
)

data class LessonClassroomStatusDto(val classroomId: Long, val classroomName: String, val status: LessonStatus)

data class ClassroomLessonStatusDto(
    val classroomId: Long,
    val courseId: Long,
    val classroomName: String,
    val currentLessonNumber: Int?,
    val latestLessonNumber: Int?,
    val isOpen: Boolean,
)
