package com.romafrihet.server.report.dto

import com.romafrihet.server.report.StudentProgressStatus

data class ClassroomProgressDto(
    val classroomId: Long,
    val classroomName: String,
    val studentCount: Int,
    val latestLessonNumber: Int?,
    val latestQuestionCount: Int,
    val latestAverageCorrect: Double?,
    val latestResponseCount: Int,
    val watchStudentCount: Int,
    val students: List<StudentProgressDto>,
)

data class StudentProgressDto(
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val recentScores: List<LessonScoreDto>,
    val latestCorrectCount: Int?,
    val previousCorrectCount: Int?,
    val totalCorrectCount: Int,
    val totalQuestionCount: Int,
    val status: StudentProgressStatus,
)

data class LessonScoreDto(
    val lessonNumber: Int,
    val correctCount: Int?,
    val questionCount: Int,
    val classAverageCorrect: Double?,
)
