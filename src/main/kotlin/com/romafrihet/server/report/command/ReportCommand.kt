package com.romafrihet.server.report.command

import java.util.UUID

class ReportCommand private constructor() {
    data class Dashboard(val userId: UUID, val courseId: Long)

    data class Scores(
        val userId: UUID,
        val courseId: Long,
        val classroomId: Long?,
        val lessonNumber: Int?,
        val page: Int,
        val size: Int,
    )

    data class Students(val userId: UUID, val classroomId: Long, val keyword: String)

    data class StudentLessonAnswers(
        val userId: UUID,
        val classroomId: Long,
        val membershipId: Long,
        val lessonNumber: Int,
    )

    data class StudentDetail(val userId: UUID, val classroomId: Long, val membershipId: Long)

    data class StudentReport(val userId: UUID, val classroomId: Long)
}
