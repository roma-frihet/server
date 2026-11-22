package com.romafrihet.server.report.command

import com.romafrihet.server.report.ScoreExportSheet
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

    /** classroomId가 null이면 수업의 모든 반이다. */
    data class ScoreExport(
        val userId: UUID,
        val courseId: Long,
        val classroomId: Long?,
        val lessonNumber: Int?,
        val sheet: ScoreExportSheet,
    )

    data class Students(val userId: UUID, val classroomId: Long, val keyword: String)

    data class StudentDetail(val userId: UUID, val classroomId: Long, val membershipId: Long)

    data class StudentReport(val userId: UUID, val classroomId: Long)
}
