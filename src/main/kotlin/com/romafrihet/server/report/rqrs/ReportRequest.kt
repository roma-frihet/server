package com.romafrihet.server.report.rqrs

import com.romafrihet.server.report.ScoreExportSheet
import com.romafrihet.server.report.command.ReportCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

private const val MAX_PAGE_SIZE = 100L
private const val DEFAULT_PAGE_SIZE = 20

class ReportRequest private constructor() {
    @Schema(name = "ReportRequestScoreQuery")
    data class ScoreQuery(
        val classroomId: Long? = null,
        val lessonNumber: Int? = null,
        @field:Min(0)
        val page: Int? = null,
        @field:Min(1)
        @field:Max(MAX_PAGE_SIZE)
        val size: Int? = null,
    ) {
        fun toCommand(userId: UUID, courseId: Long) = ReportCommand.Scores(
            userId = userId,
            courseId = courseId,
            classroomId = classroomId,
            lessonNumber = lessonNumber,
            page = page ?: 0,
            size = size ?: DEFAULT_PAGE_SIZE,
        )
    }

    @Schema(name = "ReportRequestStudentQuery")
    data class StudentQuery(
        @field:Size(max = 50)
        val keyword: String? = null,
    ) {
        fun toCommand(userId: UUID, classroomId: Long) = ReportCommand.Students(
            userId = userId,
            classroomId = classroomId,
            keyword = keyword?.trim().orEmpty(),
        )
    }

    @Schema(name = "ReportRequestScoreExportQuery")
    data class ScoreExportQuery(
        @field:Schema(description = "비우면 수업의 모든 반")
        val classroomId: Long? = null,
        @field:Schema(description = "QUESTION_RATES, ANSWER_SHEET는 필수")
        val lessonNumber: Int? = null,
        @field:NotNull
        val sheet: ScoreExportSheet? = null,
    ) {
        fun toCommand(userId: UUID, courseId: Long) = ReportCommand.ScoreExport(
            userId = userId,
            courseId = courseId,
            classroomId = classroomId,
            lessonNumber = lessonNumber,
            sheet = requireNotNull(sheet),
        )
    }
}
