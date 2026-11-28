package com.romafrihet.server.teacher

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.school.getOrThrow
import com.romafrihet.server.student.StudentLoginProperties
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.teacher.command.TeacherApplicationCommand
import com.romafrihet.server.teacher.dto.TeacherApplicationDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 계정은 Supabase Auth에서 만들고, 서버에는 교사가 되겠다는 신청만 남긴다. 승인은 관리자가 한다.
 *
 * 이메일 인증은 나중에 붙인다. 인증 결과는 [TeacherApplication.markEmailVerified]로 남기고,
 * 승인할 때 확인하는 것은 [TeacherApplicationProperties.requireEmailVerified]가 맡는다.
 */
@Service
@Transactional(readOnly = true)
class TeacherApplicationService(
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val teacherProfileRepository: TeacherProfileRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val schoolRepository: SchoolRepository,
    private val studentLoginProperties: StudentLoginProperties,
) {
    @Transactional
    fun apply(command: TeacherApplicationCommand.Apply): TeacherApplicationDto {
        val email = command.email
        assertCheck(!email.isNullOrBlank()) { ErrorCode.TEACHER_APPLICATION_EMAIL_REQUIRED }
        assertCheck(!isStudentAccount(command.userId, email)) { ErrorCode.TEACHER_APPLICATION_STUDENT_ACCOUNT }
        // 신청 기능 전에 SQL로 역할을 받은 교사는 신청 행 없이 프로필만 있다.
        assertCheck(!teacherProfileRepository.existsByUserId(command.userId)) {
            ErrorCode.TEACHER_APPLICATION_CLOSED
        }
        val school = schoolRepository.getOrThrow(command.schoolId)
        val application =
            teacherApplicationRepository.findByUserId(command.userId)?.apply { reapply(school, command) }
                ?: teacherApplicationRepository.save(TeacherApplication.create(school, command))
        return toDto(application)
    }

    fun findMine(userId: UUID): TeacherApplicationDto? = teacherApplicationRepository.findByUserId(userId)?.let(::toDto)

    /** 학생은 학번을 이메일 모양으로 바꿔 가입한다. 그 도메인이거나 학생 가입 정보가 있으면 학생이다. */
    private fun isStudentAccount(userId: UUID, email: String): Boolean =
        email.endsWith("@${studentLoginProperties.loginEmailDomain}", ignoreCase = true) ||
            studentProfileRepository.existsByUserId(userId)

    private fun toDto(application: TeacherApplication) = TeacherApplicationDto(
        id = requireNotNull(application.id),
        userId = application.userId,
        email = application.email,
        schoolId = requireNotNull(application.school.id),
        schoolName = application.school.name,
        name = application.name,
        status = application.status,
        rejectReason = application.rejectReason,
        emailVerifiedAt = application.emailVerifiedAt,
        reviewedAt = application.reviewedAt,
        createdAt = application.createdAt,
    )
}
