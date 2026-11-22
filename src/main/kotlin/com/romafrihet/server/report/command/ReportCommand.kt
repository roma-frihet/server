package com.romafrihet.server.report.command

import java.util.UUID

class ReportCommand private constructor() {
    data class Dashboard(val userId: UUID, val courseId: Long)

    data class ClassroomSummaries(val userId: UUID)

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

    /** classroomIds가 비어 있으면 수업의 모든 반이다. */
    data class QuestionStats(val userId: UUID, val courseId: Long, val lessonNumber: Int, val classroomIds: List<Long>)
}
