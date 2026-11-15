package com.romafrihet.server.lesson.projection

/** answeredCount가 0이면 풀지 않은 것이라 점수를 비워서 내린다. */
data class LessonScoreProjection(
    val classroomId: Long,
    val classNo: Int,
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val lessonNumber: Int,
    val correctCount: Long,
    val answeredCount: Long,
    val questionCount: Long,
)
