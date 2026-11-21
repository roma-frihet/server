package com.romafrihet.server.report.dto

data class StudentLessonScoreDto(
    val classroomId: Long,
    val classNo: Int,
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val lessonNumber: Int,
    val correctCount: Int?,
    val questionCount: Int?,
)
