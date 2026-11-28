package com.romafrihet.server.admin

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.security.SupabaseUser
import com.romafrihet.server.student.StudentLoginEmailPolicy
import com.romafrihet.server.student.StudentProfileService
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.teacher.TeacherApplicationRepository
import com.romafrihet.server.teacher.TeacherApplicationService
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.TeacherProfileRepository
import com.romafrihet.server.teacher.TeacherService
import com.romafrihet.server.teacher.command.TeacherApplicationCommand
import com.romafrihet.server.teacher.command.TeacherCommand
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class TeacherApplicationFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var applicationService: TeacherApplicationService

    @Autowired
    private lateinit var applicationAdminService: TeacherApplicationAdminService

    @Autowired
    private lateinit var teacherAdminService: TeacherAdminService

    @Autowired
    private lateinit var applicationRepository: TeacherApplicationRepository

    @Autowired
    private lateinit var teacherProfileRepository: TeacherProfileRepository

    @Autowired
    private lateinit var teacherService: TeacherService

    @Autowired
    private lateinit var studentProfileService: StudentProfileService

    @Autowired
    private lateinit var loginEmailPolicy: StudentLoginEmailPolicy

    @MockkBean(relaxUnitFun = true)
    private lateinit var supabaseAuthClient: SupabaseAuthClient

    private val adminId = UUID.randomUUID()
    private val applicantId = UUID.randomUUID()

    @Test
    fun `신청하면 대기이고 대기 중에 다시 신청하면 내용만 바뀐다`() {
        apply(name = "김선생")

        val again = apply(name = "김교사", email = "new@school.kr")

        assertThat(again.status).isEqualTo(TeacherApplicationStatus.PENDING)
        assertThat(again.name).isEqualTo("김교사")
        assertThat(again.email).isEqualTo("new@school.kr")
        assertThat(applicationService.findMine(applicantId)?.id).isEqualTo(again.id)
        assertThat(applicationService.findMine(UUID.randomUUID())).isNull()
    }

    @Test
    fun `이메일을 바꿔 다시 신청하면 이메일 인증이 풀린다`() {
        val application = apply(email = "a@school.kr")
        val entity = applicationRepository.findById(application.id).orElseThrow()
        entity.markEmailVerified(LocalDateTime.now())
        applicationRepository.save(entity)

        assertThat(apply(email = "A@School.kr").emailVerifiedAt).isNotNull()
        assertThat(apply(email = "b@school.kr").emailVerifiedAt).isNull()
    }

    @Test
    fun `학생 계정과 이메일이 없는 계정은 신청하지 못한다`() {
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_STUDENT_ACCOUNT) {
            apply(email = "s1-20101@STUDENTS.roma-frihet.app")
        }
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_EMAIL_REQUIRED) { apply(email = null) }

        val command =
            StudentProfileCommand.Create(applicantId, schoolId, 3, Random.nextInt(100, 100_000), 1, "학생", null)
        studentProfileService.create(command.copy(loginEmail = loginEmailPolicy.of(command)))
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_STUDENT_ACCOUNT) { apply(email = "real@school.kr") }
    }

    @Test
    fun `승인하면 교사 역할을 넣고 신청한 이름과 학교로 교사 프로필을 만든다`() {
        val application = apply(name = "박선생")

        applicationAdminService.approve(AdminCommand.ApproveTeacher(adminId, application.id))

        verify { supabaseAuthClient.updateRole(applicantId, "teacher") }
        val profile = requireNotNull(teacherProfileRepository.findByUserId(applicantId))
        assertThat(profile.name).isEqualTo("박선생")
        assertThat(profile.school.id).isEqualTo(schoolId)
        val approved = requireNotNull(applicationService.findMine(applicantId))
        assertThat(approved.status).isEqualTo(TeacherApplicationStatus.APPROVED)
        assertThat(approved.reviewedAt).isNotNull()
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_CLOSED) { apply() }
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_NOT_PENDING) {
            applicationAdminService.approve(AdminCommand.ApproveTeacher(adminId, application.id))
        }
    }

    @Test
    fun `Supabase가 실패하면 승인하지 않은 채로 남는다`() {
        val application = apply()
        every { supabaseAuthClient.updateRole(applicantId, "teacher") } throws
            BusinessException(ErrorCode.AUTH_PROVIDER_UNAVAILABLE)

        assertErrorCode(ErrorCode.AUTH_PROVIDER_UNAVAILABLE) {
            applicationAdminService.approve(AdminCommand.ApproveTeacher(adminId, application.id))
        }

        assertThat(applicationService.findMine(applicantId)?.status).isEqualTo(TeacherApplicationStatus.PENDING)
        assertThat(teacherProfileRepository.existsByUserId(applicantId)).isFalse()
    }

    @Test
    fun `거절하면 사유가 남고 다시 신청하면 대기로 돌아간다`() {
        val application = apply()

        applicationAdminService.reject(AdminCommand.RejectTeacher(adminId, application.id, "재직 확인이 안 됩니다"))

        val rejected = requireNotNull(applicationService.findMine(applicantId))
        assertThat(rejected.status).isEqualTo(TeacherApplicationStatus.REJECTED)
        assertThat(rejected.rejectReason).isEqualTo("재직 확인이 안 됩니다")
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_NOT_PENDING) {
            applicationAdminService.reject(AdminCommand.RejectTeacher(adminId, application.id, null))
        }
        val reapplied = apply()
        assertThat(reapplied.status).isEqualTo(TeacherApplicationStatus.PENDING)
        assertThat(reapplied.rejectReason).isNull()
    }

    @Test
    fun `대기 목록은 오래 기다린 신청부터 내린다`() {
        val first = apply()
        val second =
            applicationService.apply(TeacherApplicationCommand.Apply(UUID.randomUUID(), "b@school.kr", schoolId, "나"))

        val pending =
            applicationAdminService
                .findAll(AdminCommand.TeacherApplicationQuery(TeacherApplicationStatus.PENDING, 0, 100))
                .content
                .map { it.id }

        assertThat(pending).containsSubsequence(first.id, second.id)
    }

    @Test
    fun `교사 목록은 이름이나 신청 이메일로 찾는다`() {
        val email = "Find${Random.nextInt(1_000_000)}@School.kr"
        val application = apply(name = "검색선생", email = email)
        applicationAdminService.approve(AdminCommand.ApproveTeacher(adminId, application.id))

        val byEmail = teacherAdminService.findAll(AdminCommand.TeacherQuery(null, email.lowercase(), 0, 20)).content
        assertThat(byEmail.map { it.userId }).containsExactly(applicantId)
        val byName = teacherAdminService.findAll(AdminCommand.TeacherQuery(null, "검색선생", 0, 100)).content
        assertThat(byName.map { it.userId }).contains(applicantId)
    }

    @Test
    fun `관리자는 자기 신청을 승인하지 못한다`() {
        val application = apply()

        assertErrorCode(ErrorCode.ADMIN_SELF_ACTION) {
            applicationAdminService.approve(AdminCommand.ApproveTeacher(applicantId, application.id))
        }
    }

    @Test
    fun `신청 없이 등록된 예전 교사도 권한을 거두고 다시 승인할 수 있다`() {
        teacherService.saveProfile(TeacherCommand.SaveProfile(applicantId, schoolId, "예전선생"))
        every { supabaseAuthClient.findUser(applicantId) } returns SupabaseUser(email = "old@school.kr")

        teacherAdminService.revoke(AdminCommand.RevokeTeacher(adminId, applicantId))

        verify { supabaseAuthClient.updateRole(applicantId, null) }
        val revoked = requireNotNull(applicationRepository.findByUserId(applicantId))
        assertThat(revoked.status).isEqualTo(TeacherApplicationStatus.REVOKED)
        assertThat(revoked.email).isEqualTo("old@school.kr")
        assertErrorCode(ErrorCode.TEACHER_APPLICATION_CLOSED) { apply() }

        applicationAdminService.approve(AdminCommand.ApproveTeacher(adminId, requireNotNull(revoked.id)))

        assertThat(applicationService.findMine(applicantId)?.status).isEqualTo(TeacherApplicationStatus.APPROVED)
        assertThat(teacherProfileRepository.findByUserId(applicantId)?.name).isEqualTo("예전선생")
    }

    @Test
    fun `교사가 아닌 계정이나 자기 계정은 권한을 거두지 못한다`() {
        assertErrorCode(ErrorCode.NOT_FOUND) {
            teacherAdminService.revoke(AdminCommand.RevokeTeacher(adminId, UUID.randomUUID()))
        }
        assertErrorCode(ErrorCode.ADMIN_SELF_ACTION) {
            teacherAdminService.revoke(AdminCommand.RevokeTeacher(adminId, adminId))
        }
    }

    private fun apply(name: String = "김선생", email: String? = "teacher@school.kr") =
        applicationService.apply(TeacherApplicationCommand.Apply(applicantId, email, schoolId, name))
}
