package com.romafrihet.server.admin.dto

import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.teacher.TeacherApplicationStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * 신청 행이 없는 교사는 신청 기능 전에 SQL로 역할을 받은 교사다. 승인된 것으로 보고 email과 applicationId를 비운다.
 */
data class AdminTeacherDto(
    val userId: UUID,
    val name: String,
    val schoolId: Long,
    val schoolName: String,
    val email: String?,
    val applicationId: Long?,
    val status: TeacherApplicationStatus,
    val courseCount: Long,
    val isSuspended: Boolean,
    val createdAt: LocalDateTime,
)

data class AdminStudentDto(
    val userId: UUID,
    val schoolId: Long,
    val schoolName: String,
    val grade: Int,
    val classNumber: Int,
    val seatNumber: Int,
    val studentNumber: String,
    val name: String,
    val mustChangePassword: Boolean,
    val isSuspended: Boolean,
    val createdAt: LocalDateTime,
)

data class AdminQuestionDto(
    val questionId: Long,
    val courseId: Long,
    val teacherUserId: UUID,
    val teacherName: String?,
    val schoolName: String,
    val grade: Int,
    val subjectName: String,
    val lessonNumber: Int,
    val lessonTitle: String,
    val type: QuestionType,
    val content: String?,
    val hasImage: Boolean,
    val createdAt: LocalDateTime,
)
