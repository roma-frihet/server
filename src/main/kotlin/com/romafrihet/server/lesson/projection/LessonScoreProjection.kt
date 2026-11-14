package com.romafrihet.server.lesson.projection

data class LessonScoreProjection(
    val classroomId: Long,
    val classNo: Int,
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val lessonNumber: Int,
    val correctCount: Int?,
    val questionCount: Int?,
)
