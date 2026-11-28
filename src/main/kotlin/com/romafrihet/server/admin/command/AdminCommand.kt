package com.romafrihet.server.admin.command

import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.teacher.TeacherApplicationStatus
import java.util.UUID

/** adminId는 요청한 관리자다. 자기 계정에 하는 작업을 막고 처리 기록을 남길 때 쓴다. */
class AdminCommand private constructor() {
    data class TeacherApplicationQuery(val status: TeacherApplicationStatus?, val page: Int, val size: Int)

    data class ApproveTeacher(val adminId: UUID, val applicationId: Long)

    data class RejectTeacher(val adminId: UUID, val applicationId: Long, val reason: String?)

    data class TeacherQuery(val schoolId: Long?, val keyword: String?, val page: Int, val size: Int)

    data class RevokeTeacher(val adminId: UUID, val teacherUserId: UUID)

    data class StudentQuery(
        val schoolId: Long?,
        val grade: Int?,
        val classNumber: Int?,
        val keyword: String?,
        val page: Int,
        val size: Int,
    )

    data class Suspend(val adminId: UUID, val userId: UUID, val reason: String?)

    data class Unsuspend(val adminId: UUID, val userId: UUID)

    data class QuestionQuery(
        val schoolId: Long?,
        val subjectId: Long?,
        val teacherUserId: UUID?,
        val type: QuestionType?,
        val keyword: String?,
        val page: Int,
        val size: Int,
    )
}
