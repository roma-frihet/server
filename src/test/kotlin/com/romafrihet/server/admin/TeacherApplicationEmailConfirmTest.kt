package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.school.School
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.teacher.TeacherApplication
import com.romafrihet.server.teacher.TeacherApplicationProperties
import com.romafrihet.server.teacher.TeacherApplicationRepository
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.TeacherProfileRepository
import com.romafrihet.server.teacher.command.TeacherApplicationCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

/** 이메일 인증 요구는 설정으로 켜므로 DB 없이 설정 값만 바꿔 본다. */
class TeacherApplicationEmailConfirmTest {
    private val applicantId = UUID.randomUUID()
    private val applicationRepository = mockk<TeacherApplicationRepository>()
    private val teacherProfileRepository = mockk<TeacherProfileRepository>()
    private val supabaseAuthClient = mockk<SupabaseAuthClient>(relaxUnitFun = true)
    private val application =
        TeacherApplication.create(
            mockk<School>(relaxed = true),
            TeacherApplicationCommand.Apply(applicantId, "a@school.kr", 1, "김선생"),
        )
    private val service =
        TeacherApplicationAdminService(
            teacherApplicationRepository = applicationRepository,
            teacherProfileRepository = teacherProfileRepository,
            teacherService = mockk(),
            supabaseAuthClient = supabaseAuthClient,
            properties = TeacherApplicationProperties(requireEmailVerified = true),
            clock = Clock.systemDefaultZone(),
            transactionManager = mockk(relaxed = true),
        )

    @Test
    fun `인증을 요구하면 인증하지 않은 신청은 역할을 넣기 전에 거절한다`() {
        every { applicationRepository.findById(1) } returns Optional.of(application)

        assertThatThrownBy { service.approve(AdminCommand.ApproveTeacher(UUID.randomUUID(), 1)) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.TEACHER_APPLICATION_EMAIL_NOT_CONFIRMED)
            }
        verify(exactly = 0) { supabaseAuthClient.updateRole(any(), any()) }
    }

    @Test
    fun `인증을 마친 신청은 승인한다`() {
        application.markEmailVerified(LocalDateTime.now())
        every { applicationRepository.findById(1) } returns Optional.of(application)
        every { teacherProfileRepository.existsByUserId(applicantId) } returns true

        service.approve(AdminCommand.ApproveTeacher(UUID.randomUUID(), 1))

        verify { supabaseAuthClient.updateRole(applicantId, "teacher") }
        assertThat(application.status).isEqualTo(TeacherApplicationStatus.APPROVED)
    }
}
