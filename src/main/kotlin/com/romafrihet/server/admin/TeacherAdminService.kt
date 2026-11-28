package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.admin.dto.AdminTeacherDto
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.teacher.TeacherApplication
import com.romafrihet.server.teacher.TeacherApplicationRepository
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.TeacherProfileRepository
import com.romafrihet.server.teacher.dto.TeacherProfileQueryDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

/**
 * 교사 목록은 교사 프로필 기준이다. 권한을 거둬도 프로필과 수업은 남겨 두어, 다시 승인하면 그대로 돌아온다.
 */
@Service
class TeacherAdminService(
    private val teacherProfileRepository: TeacherProfileRepository,
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val courseRepository: CourseRepository,
    private val accountSuspensionRepository: AccountSuspensionRepository,
    private val supabaseAuthClient: SupabaseAuthClient,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val readOnlyTemplate = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    fun findAll(command: AdminCommand.TeacherQuery): Page<AdminTeacherDto> = requireNotNull(
        readOnlyTemplate.execute {
            val page =
                teacherProfileRepository.queryProfiles(
                    TeacherProfileQueryDto(schoolId = command.schoolId, keyword = command.keyword),
                    PageRequest.of(command.page, command.size),
                )
            val userIds = page.content.map { it.userId }
            val applications = teacherApplicationRepository.findAllByUserIdIn(userIds).associateBy { it.userId }
            val courseCounts = courseRepository.findCourseCounts(userIds).associate { it.userId to it.courseCount }
            val suspended = accountSuspensionRepository.findAllByUserIdIn(userIds).map { it.userId }.toSet()
            page.map { profile ->
                val application = applications[profile.userId]
                AdminTeacherDto(
                    userId = profile.userId,
                    name = profile.name,
                    schoolId = profile.schoolId,
                    schoolName = profile.schoolName,
                    email = application?.email,
                    applicationId = application?.id,
                    status = application?.status ?: TeacherApplicationStatus.APPROVED,
                    courseCount = courseCounts[profile.userId] ?: 0,
                    isSuspended = profile.userId in suspended,
                    createdAt = profile.createdAt,
                )
            }
        },
    )

    /**
     * 역할을 지우고 신청을 회수 상태로 바꾼다. 교사가 세션을 갱신하거나 토큰이 만료되면 교사 API가 막힌다.
     * 신청 행이 없는 예전 교사는 기록을 남기려고 Supabase에서 이메일을 읽어 승인된 신청을 먼저 만든다.
     */
    fun revoke(command: AdminCommand.RevokeTeacher) {
        assertCheck(command.teacherUserId != command.adminId) { ErrorCode.ADMIN_SELF_ACTION }
        val hasApplication =
            requireNotNull(
                readOnlyTemplate.execute {
                    teacherProfileRepository.findByUserId(command.teacherUserId)
                        ?: throw BusinessException(ErrorCode.NOT_FOUND)
                    teacherApplicationRepository.findByUserId(command.teacherUserId)?.also {
                        assertCheck(it.status == TeacherApplicationStatus.APPROVED) {
                            ErrorCode.TEACHER_APPLICATION_NOT_PENDING
                        }
                    } != null
                },
            )
        val legacyEmail = if (hasApplication) null else supabaseAuthClient.findUser(command.teacherUserId).email
        supabaseAuthClient.updateRole(command.teacherUserId, null)
        transactionTemplate.executeWithoutResult {
            val now = LocalDateTime.now(clock)
            val application =
                teacherApplicationRepository.findByUserId(command.teacherUserId)
                    ?: teacherApplicationRepository.save(
                        TeacherApplication.approvedLegacy(
                            profile = requireNotNull(teacherProfileRepository.findByUserId(command.teacherUserId)),
                            email = legacyEmail.orEmpty(),
                            now = now,
                        ),
                    )
            application.revoke(command.adminId, now)
        }
    }
}
