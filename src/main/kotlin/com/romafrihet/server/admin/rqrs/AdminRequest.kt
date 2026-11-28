package com.romafrihet.server.admin.rqrs

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.teacher.TeacherApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.util.UUID

private const val MAX_PAGE_SIZE = 100L
private const val DEFAULT_PAGE_SIZE = 20

class AdminRequest private constructor() {
    @Schema(name = "AdminRequestTeacherApplicationQuery")
    data class TeacherApplicationQuery(
        @field:Schema(description = "비우면 모든 상태. 기본 화면은 PENDING")
        val status: TeacherApplicationStatus? = null,
        @field:Min(0)
        val page: Int? = null,
        @field:Min(1)
        @field:Max(MAX_PAGE_SIZE)
        val size: Int? = null,
    ) {
        fun toCommand() = AdminCommand.TeacherApplicationQuery(
            status = status,
            page = page ?: 0,
            size = size ?: DEFAULT_PAGE_SIZE,
        )
    }

    @Schema(name = "AdminRequestRejectTeacher")
    data class RejectTeacher(
        @field:Size(max = 200)
        @field:Schema(description = "신청한 사람이 보게 되는 사유")
        val reason: String? = null,
    ) {
        fun toCommand(adminId: UUID, applicationId: Long) = AdminCommand.RejectTeacher(
            adminId = adminId,
            applicationId = applicationId,
            reason = reason?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    @Schema(name = "AdminRequestTeacherQuery")
    data class TeacherQuery(
        val schoolId: Long? = null,
        @field:Size(max = 50)
        @field:Schema(description = "이름이나 이메일 일부. 신청 기록이 없는 예전 교사는 이름으로만 찾는다")
        val keyword: String? = null,
        @field:Min(0)
        val page: Int? = null,
        @field:Min(1)
        @field:Max(MAX_PAGE_SIZE)
        val size: Int? = null,
    ) {
        fun toCommand() = AdminCommand.TeacherQuery(
            schoolId = schoolId,
            keyword = keyword,
            page = page ?: 0,
            size = size ?: DEFAULT_PAGE_SIZE,
        )
    }

    @Schema(name = "AdminRequestStudentQuery")
    data class StudentQuery(
        val schoolId: Long? = null,
        @field:Min(1)
        val grade: Int? = null,
        @field:Min(1)
        val classNumber: Int? = null,
        @field:Size(max = 50)
        @field:Schema(description = "이름 일부")
        val keyword: String? = null,
        @field:Min(0)
        val page: Int? = null,
        @field:Min(1)
        @field:Max(MAX_PAGE_SIZE)
        val size: Int? = null,
    ) {
        fun toCommand() = AdminCommand.StudentQuery(
            schoolId = schoolId,
            grade = grade,
            classNumber = classNumber,
            keyword = keyword,
            page = page ?: 0,
            size = size ?: DEFAULT_PAGE_SIZE,
        )
    }

    @Schema(name = "AdminRequestSuspend")
    data class Suspend(
        @field:Size(max = 200)
        val reason: String? = null,
    ) {
        fun toCommand(adminId: UUID, userId: UUID) = AdminCommand.Suspend(
            adminId = adminId,
            userId = userId,
            reason = reason?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    @Schema(name = "AdminRequestQuestionQuery")
    data class QuestionQuery(
        val schoolId: Long? = null,
        val subjectId: Long? = null,
        val teacherUserId: UUID? = null,
        val type: QuestionType? = null,
        @field:Size(max = 100)
        @field:Schema(description = "문제 글 일부")
        val keyword: String? = null,
        @field:Min(0)
        val page: Int? = null,
        @field:Min(1)
        @field:Max(MAX_PAGE_SIZE)
        val size: Int? = null,
    ) {
        fun toCommand() = AdminCommand.QuestionQuery(
            schoolId = schoolId,
            subjectId = subjectId,
            teacherUserId = teacherUserId,
            type = type,
            keyword = keyword,
            page = page ?: 0,
            size = size ?: DEFAULT_PAGE_SIZE,
        )
    }
}
