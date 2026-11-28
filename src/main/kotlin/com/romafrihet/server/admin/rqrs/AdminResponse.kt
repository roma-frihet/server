package com.romafrihet.server.admin.rqrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.romafrihet.server.admin.dto.AdminQuestionDto
import com.romafrihet.server.admin.dto.AdminStudentDto
import com.romafrihet.server.admin.dto.AdminTeacherDto
import com.romafrihet.server.common.web.PageInfo
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.dto.TeacherApplicationDto
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.LocalDateTime
import java.util.UUID

class AdminResponse private constructor() {
    @Schema(name = "AdminResponseTeacherApplication")
    data class TeacherApplication(
        val id: Long,
        val userId: UUID,
        val email: String,
        val schoolId: Long,
        val schoolName: String,
        val name: String,
        val status: TeacherApplicationStatus,
        val rejectReason: String?,
        @field:Schema(description = "이메일 인증을 마친 시각. 인증 기능을 붙이기 전에는 null")
        val emailVerifiedAt: LocalDateTime?,
        val reviewedAt: LocalDateTime?,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: TeacherApplicationDto) = TeacherApplication(
                id = dto.id,
                userId = dto.userId,
                email = dto.email,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                name = dto.name,
                status = dto.status,
                rejectReason = dto.rejectReason,
                emailVerifiedAt = dto.emailVerifiedAt,
                reviewedAt = dto.reviewedAt,
                createdAt = dto.createdAt,
            )
        }
    }

    @Schema(name = "AdminResponseTeacherApplicationResultPage")
    data class TeacherApplicationResultPage(val content: List<TeacherApplication>, val pageInfo: PageInfo) {
        companion object {
            fun of(page: Page<TeacherApplicationDto>) = TeacherApplicationResultPage(
                content = page.content.map(TeacherApplication::of),
                pageInfo = PageInfo.of(page),
            )
        }
    }

    @Schema(name = "AdminResponseTeacher")
    data class Teacher(
        val userId: UUID,
        val name: String,
        val schoolId: Long,
        val schoolName: String,
        @field:Schema(description = "신청 기능 전에 등록한 교사는 null")
        val email: String?,
        @field:Schema(description = "권한을 다시 줄 때 승인 API에 쓴다. 신청 기능 전에 등록한 교사는 null")
        val applicationId: Long?,
        @field:Schema(description = "APPROVED 교사, REVOKED 권한 회수")
        val status: TeacherApplicationStatus,
        val courseCount: Long,
        @get:JsonProperty("isSuspended")
        val isSuspended: Boolean,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: AdminTeacherDto) = Teacher(
                userId = dto.userId,
                name = dto.name,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                email = dto.email,
                applicationId = dto.applicationId,
                status = dto.status,
                courseCount = dto.courseCount,
                isSuspended = dto.isSuspended,
                createdAt = dto.createdAt,
            )
        }
    }

    @Schema(name = "AdminResponseTeacherResultPage")
    data class TeacherResultPage(val content: List<Teacher>, val pageInfo: PageInfo) {
        companion object {
            fun of(page: Page<AdminTeacherDto>) =
                TeacherResultPage(content = page.content.map(Teacher::of), pageInfo = PageInfo.of(page))
        }
    }

    @Schema(name = "AdminResponseStudent")
    data class Student(
        val userId: UUID,
        val schoolId: Long,
        val schoolName: String,
        val grade: Int,
        val classNumber: Int,
        val seatNumber: Int,
        val studentNumber: String,
        val name: String,
        @field:Schema(description = "임시 비밀번호를 받고 아직 바꾸지 않았다")
        val mustChangePassword: Boolean,
        @get:JsonProperty("isSuspended")
        val isSuspended: Boolean,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: AdminStudentDto) = Student(
                userId = dto.userId,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                grade = dto.grade,
                classNumber = dto.classNumber,
                seatNumber = dto.seatNumber,
                studentNumber = dto.studentNumber,
                name = dto.name,
                mustChangePassword = dto.mustChangePassword,
                isSuspended = dto.isSuspended,
                createdAt = dto.createdAt,
            )
        }
    }

    @Schema(name = "AdminResponseStudentResultPage")
    data class StudentResultPage(val content: List<Student>, val pageInfo: PageInfo) {
        companion object {
            fun of(page: Page<AdminStudentDto>) =
                StudentResultPage(content = page.content.map(Student::of), pageInfo = PageInfo.of(page))
        }
    }

    @Schema(name = "AdminResponseTemporaryPassword")
    data class TemporaryPassword(
        @field:Schema(description = "한 번만 내려준다. 학생에게 전달한 뒤 화면에서 지운다")
        val temporaryPassword: String,
    )

    @Schema(name = "AdminResponseQuestion")
    data class Question(
        val questionId: Long,
        val courseId: Long,
        val teacherUserId: UUID,
        @field:Schema(description = "교사 프로필이 없으면 null")
        val teacherName: String?,
        val schoolName: String,
        val grade: Int,
        val subjectName: String,
        val lessonNumber: Int,
        val lessonTitle: String,
        val type: QuestionType,
        @field:Schema(description = "이미지만 있는 문제는 null")
        val content: String?,
        val hasImage: Boolean,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: AdminQuestionDto) = Question(
                questionId = dto.questionId,
                courseId = dto.courseId,
                teacherUserId = dto.teacherUserId,
                teacherName = dto.teacherName,
                schoolName = dto.schoolName,
                grade = dto.grade,
                subjectName = dto.subjectName,
                lessonNumber = dto.lessonNumber,
                lessonTitle = dto.lessonTitle,
                type = dto.type,
                content = dto.content,
                hasImage = dto.hasImage,
                createdAt = dto.createdAt,
            )
        }
    }

    @Schema(name = "AdminResponseQuestionResultPage")
    data class QuestionResultPage(val content: List<Question>, val pageInfo: PageInfo) {
        companion object {
            fun of(page: Page<AdminQuestionDto>) =
                QuestionResultPage(content = page.content.map(Question::of), pageInfo = PageInfo.of(page))
        }
    }
}
