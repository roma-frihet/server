package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.School
import com.romafrihet.server.student.StudentProfile
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.student.command.StudentProfileCommand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

class ClassroomMembershipServiceTest {
    private val classroomRepository = mockk<ClassroomRepository>()
    private val membershipRepository = mockk<ClassroomMembershipRepository>()
    private val profileRepository = mockk<StudentProfileRepository>()
    private val service = ClassroomMembershipService(classroomRepository, membershipRepository, profileRepository)

    private val teacherId = UUID.randomUUID()
    private val studentId = UUID.randomUUID()
    private val school = mockk<School>(relaxed = true)
    private val command = ClassroomMembershipCommand.Join(studentId, CODE)

    @BeforeEach
    fun setUp() {
        every { profileRepository.findByUserId(studentId) } returns
            StudentProfile.create(school, StudentProfileCommand.Create(studentId, 1L, 2, 1, 1, "홍길동"))
    }

    @Test
    fun `유효한 코드로 요청하면 가입 정보의 이름과 학번이 담긴 대기 요청을 만든다`() {
        val classroom = classroomWithCode(LocalDateTime.now().plusMinutes(10))
        every { classroomRepository.findByJoinCode(CODE) } returns classroom
        every { membershipRepository.findByClassroomIdAndUserId(1L, studentId) } returns null
        every { membershipRepository.save(any()) } answers {
            firstArg<ClassroomMembership>().also { ReflectionTestUtils.setField(it, "id", 10L) }
        }

        val result = service.join(command)

        assertThat(result.studentName).isEqualTo("홍길동")
        assertThat(result.studentNumber).isEqualTo("20101")
        assertThat(result.status).isEqualTo(MembershipStatus.PENDING)
    }

    @Test
    fun `가입 정보가 없으면 합류 요청을 거절한다`() {
        every { profileRepository.findByUserId(studentId) } returns null

        assertThatThrownBy { service.join(command) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.STUDENT_PROFILE_REQUIRED)
            }
    }

    @Test
    fun `거절된 학생은 다시 요청할 수 있다`() {
        val classroom = classroomWithCode(LocalDateTime.now().plusMinutes(10))
        val rejected = membership(classroom).apply { reject() }
        every { classroomRepository.findByJoinCode(CODE) } returns classroom
        every { membershipRepository.findByClassroomIdAndUserId(1L, studentId) } returns rejected

        assertThat(service.join(command).status).isEqualTo(MembershipStatus.PENDING)
    }

    @Test
    fun `만료된 코드는 없는 코드와 구분해서 거절한다`() {
        every { classroomRepository.findByJoinCode(CODE) } returns
            classroomWithCode(LocalDateTime.now().minusSeconds(1))

        assertThatThrownBy { service.join(command) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.JOIN_CODE_EXPIRED)
            }
    }

    @Test
    fun `없는 코드는 거절한다`() {
        every { classroomRepository.findByJoinCode(CODE) } returns null

        assertThatThrownBy { service.join(command) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.JOIN_CODE_INVALID)
            }
    }

    @Test
    fun `이미 승인된 학생의 재요청은 거절한다`() {
        val classroom = classroomWithCode(LocalDateTime.now().plusMinutes(10))
        val approved = membership(classroom).apply { approve() }
        every { classroomRepository.findByJoinCode(CODE) } returns classroom
        every { membershipRepository.findByClassroomIdAndUserId(1L, studentId) } returns approved

        assertThatThrownBy { service.join(command) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.MEMBERSHIP_ALREADY_REQUESTED)
            }
    }

    private fun classroomWithCode(expiresAt: LocalDateTime): Classroom =
        Classroom.create(course(), ClassroomCommand.Create(teacherId, 1L, "1반")).apply {
            ReflectionTestUtils.setField(this, "id", 1L)
            issueJoinCode(CODE, expiresAt)
        }

    private fun course(): Course = Course.create(school, CourseCommand.Create(teacherId, 1L, 2, "과학"))

    private fun membership(classroom: Classroom): ClassroomMembership = ClassroomMembership
        .create(classroom, ClassroomMembershipCommand.Create(studentId, "홍길동", "20101"))
        .also { ReflectionTestUtils.setField(it, "id", 10L) }

    private companion object {
        const val CODE = "ABCD23"
    }
}
