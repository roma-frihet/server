package com.romafrihet.server.report.dto

data class ClassroomSummaryDto(
    val classroomId: Long,
    val courseId: Long,
    val grade: Int,
    val subjectName: String,
    val classNo: Int,
    val studentCount: Int,
    val openLesson: ClassroomOpenLessonDto?,
    val lastFinishedLessonNumber: Int?,
    val latestLessonNumber: Int?,
    val latestQuestionCount: Int,
    val latestAverageCorrect: Double?,
    val watchStudentCount: Int,
)

data class ClassroomOpenLessonDto(
    val lessonNumber: Int,
    val title: String,
    val questionCount: Int,
    val responseCount: Int,
)
