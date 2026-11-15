package com.romafrihet.server.teacher

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.student.StudentPasswordService
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.student.StudentProfileService
import com.romafrihet.server.student.command.StudentPasswordCommand
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.teacher.command.TeacherCommand
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

class StudentPasswordResetFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var resetService: StudentPasswordResetService

    @Autowired
    private lateinit var passwordService: StudentPasswordService

    @Autowired
    private lateinit var profileService: StudentProfileService

    @Autowired
    private lateinit var profileRepository: StudentProfileRepository

    @MockkBean(relaxUnitFun = true)
    private lateinit var supabaseAuthClient: SupabaseAuthClient

    @BeforeEach
    fun saveStudentProfile() {
        profileService.create(
            StudentProfileCommand.Create(studentId, schoolId, 2, 1, seatNumbers.incrementAndGet(), "홍길동"),
        )
    }

    @Test
    fun `초기화하면 임시 비밀번호를 한 번 내리고 변경 표시를 남긴다`() {
        val classroomId = joinedClassroom()
        val password = slot<String>()
        justRun { supabaseAuthClient.updatePassword(studentId, capture(password)) }

        val temporary = resetService.reset(resetCommand(classroomId))

        assertThat(temporary).hasSize(8)
        assertThat(password.captured).isEqualTo(temporary)
        assertThat(requireNotNull(profileRepository.findByUserId(studentId)).mustChangePassword).isTrue()
    }

    @Test
    fun `학생이 새 비밀번호를 정하면 변경 표시가 풀린다`() {
        val classroomId = joinedClassroom()
        resetService.reset(resetCommand(classroomId))

        passwordService.change(StudentPasswordCommand.Change(studentId, "newpassword"))

        verify { supabaseAuthClient.updatePassword(studentId, "newpassword") }
        assertThat(requireNotNull(profileRepository.findByUserId(studentId)).mustChangePassword).isFalse()
    }

    /** Supabase 호출이 트랜잭션을 붙잡으면 안 된다. */
    @Test
    fun `Supabase 호출은 트랜잭션 밖에서 한다`() {
        val classroomId = joinedClassroom()
        var activeDuringCall: Boolean? = null
        justRun { supabaseAuthClient.updatePassword(any(), any()) }
        io.mockk.every { supabaseAuthClient.updatePassword(any(), any()) } answers {
            activeDuringCall = TransactionSynchronizationManager.isActualTransactionActive()
        }

        resetService.reset(resetCommand(classroomId))

        assertThat(activeDuringCall).isFalse()
    }

    /** 프론트가 비밀번호 변경 화면으로 보내려면 이 값을 읽을 수 있어야 한다. */
    @Test
    fun `내 정보 조회에 임시 비밀번호 상태가 담긴다`() {
        val classroomId = joinedClassroom()
        assertThat(requireNotNull(profileService.find(studentId)).mustChangePassword).isFalse()

        resetService.reset(resetCommand(classroomId))

        assertThat(requireNotNull(profileService.find(studentId)).mustChangePassword).isTrue()
    }

    @Test
    fun `다른 교사는 초기화할 수 없다`() {
        val classroomId = joinedClassroom()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            resetService.reset(
                TeacherCommand.ResetStudentPassword(UUID.randomUUID(), classroomId, membershipId(classroomId)),
            )
        }
    }

    @Test
    fun `가입 정보가 없는 사용자는 비밀번호를 바꿀 수 없다`() {
        assertErrorCode(ErrorCode.STUDENT_PROFILE_REQUIRED) {
            passwordService.change(StudentPasswordCommand.Change(UUID.randomUUID(), "newpassword"))
        }
    }

    private fun resetCommand(classroomId: Long) =
        TeacherCommand.ResetStudentPassword(teacherId, classroomId, membershipId(classroomId))

    private fun membershipId(classroomId: Long): Long =
        requireNotNull(membershipRepository.findAllByClassroomIdOrderByIdAsc(classroomId).single().id)

    private companion object {
        val seatNumbers = java.util.concurrent.atomic.AtomicInteger(50)
    }
}
