package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.teacher.TeacherApplication
import com.romafrihet.server.teacher.TeacherApplicationProperties
import com.romafrihet.server.teacher.TeacherApplicationRepository
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.TeacherProfileRepository
import com.romafrihet.server.teacher.TeacherService
import com.romafrihet.server.teacher.command.TeacherCommand
import com.romafrihet.server.teacher.dto.TeacherApplicationDto
import com.romafrihet.server.teacher.getOrThrow
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime

/**
 * 승인하면 Supabase app_metadata.role에 teacher를 넣고 신청한 이름과 학교로 교사 프로필을 만든다.
 * Supabase 호출은 트랜잭션 밖에서 먼저 한다. 그 뒤 저장이 실패해도 다시 승인하면 같은 결과가 된다.
 */
@Service
class TeacherApplicationAdminService(
    private val teacherApplicationRepository: TeacherApplicationRepository,
    private val teacherProfileRepository: TeacherProfileRepository,
    private val teacherService: TeacherService,
    private val supabaseAuthClient: SupabaseAuthClient,
    private val properties: TeacherApplicationProperties,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply { isReadOnly = false }
    private val readOnlyTemplate = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    /** 오래 기다린 신청부터 본다. 상태를 비우면 모두 본다. */
    fun findAll(command: AdminCommand.TeacherApplicationQuery): Page<TeacherApplicationDto> = inReadOnly {
        teacherApplicationRepository
            .findAllByStatusIn(
                command.status?.let(::listOf) ?: TeacherApplicationStatus.entries,
                PageRequest.of(command.page, command.size, Sort.by("createdAt", "id")),
            ).map(::toDto)
    }

    fun approve(command: AdminCommand.ApproveTeacher) {
        val userId =
            inReadOnly {
                val application = teacherApplicationRepository.getOrThrow(command.applicationId)
                assertCheck(application.status != TeacherApplicationStatus.APPROVED) {
                    ErrorCode.TEACHER_APPLICATION_NOT_PENDING
                }
                assertCheck(!properties.requireEmailVerified || application.emailVerifiedAt != null) {
                    ErrorCode.TEACHER_APPLICATION_EMAIL_NOT_CONFIRMED
                }
                application.userId
            }
        // 관리자 자신의 역할을 teacher로 덮어쓰면 관리자 권한을 잃는다.
        assertCheck(userId != command.adminId) { ErrorCode.ADMIN_SELF_ACTION }
        supabaseAuthClient.updateRole(userId, TEACHER_ROLE)
        inTransaction {
            val application = teacherApplicationRepository.getOrThrow(command.applicationId)
            application.approve(command.adminId, now())
            if (!teacherProfileRepository.existsByUserId(userId)) {
                teacherService.saveProfile(
                    TeacherCommand.SaveProfile(
                        userId = userId,
                        schoolId = requireNotNull(application.school.id),
                        name = application.name,
                    ),
                )
            }
        }
    }

    fun reject(command: AdminCommand.RejectTeacher) = inTransaction {
        teacherApplicationRepository.getOrThrow(command.applicationId).reject(command.adminId, command.reason, now())
    }

    private fun now() = LocalDateTime.now(clock)

    private fun <T> inReadOnly(block: () -> T): T = requireNotNull(readOnlyTemplate.execute { block() })

    private fun inTransaction(block: () -> Unit) = transactionTemplate.executeWithoutResult { block() }

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

    private companion object {
        const val TEACHER_ROLE = "teacher"
    }
}
