package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.classroom.event.ClassroomMembershipRemovedEvent
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.School
import com.romafrihet.server.student.StudentProfile
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.subject.Subject
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ClassroomMembershipServiceTest {
    private val classroomRepository = mockk<ClassroomRepository>()
    private val membershipRepository = mockk<ClassroomMembershipRepository>()
    private val profileRepository = mockk<StudentProfileRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>()
    private val clock = Clock.fixed(Instant.parse("2026-11-15T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = ClassroomMembershipService(
        classroomRepository,
        membershipRepository,
        profileRepository,
        eventPublisher,
        clock,
    )

    private val teacherId = UUID.randomUUID()
    private val studentId = UUID.randomUUID()
    private val school = mockk<School>(relaxed = true)
    private val command = ClassroomMembershipCommand.Join(studentId, CODE)

    @BeforeEach
    fun setUp() {
        every { profileRepository.findByUserId(studentId) } returns
            StudentProfile.create(school, StudentProfileCommand.Create(studentId, 1L, 2, 1, 1, "홍길동", null))
    }

    @Test
    fun `유효한 코드를 넣으면 승인 없이 바로 연결된다`() {
        givenClassroomWithCode(now().plusMinutes(1))
        every { membershipRepository.existsByClassroomCourseIdAndUserId(COURSE_ID, studentId) } returns false
        every { membershipRepository.save(any()) } answers {
            firstArg<ClassroomMembership>().also { ReflectionTestUtils.setField(it, "id", MEMBERSHIP_ID) }
        }

        val result = service.join(command)

        assertThat(result.studentName).isEqualTo("홍길동")
        assertThat(result.studentNumber).isEqualTo("20101")
        assertThat(result.classNo).isEqualTo(1)
    }

    @Test
    fun `학생을 내보내면 기록을 지우라는 이벤트가 나간다`() {
        val membership = membership()
        every { classroomRepository.findByIdAndCourseUserId(CLASSROOM_ID, teacherId) } returns membership.classroom
        every { membershipRepository.findByIdAndClassroomId(MEMBERSHIP_ID, CLASSROOM_ID) } returns membership
        justRun { membershipRepository.delete(membership) }
        justRun { eventPublisher.publishEvent(any<ClassroomMembershipRemovedEvent>()) }

        service.removeStudent(ClassroomMembershipCommand.RemoveStudent(teacherId, CLASSROOM_ID, MEMBERSHIP_ID))

        verify { membershipRepository.delete(membership) }
        verify { eventPublisher.publishEvent(ClassroomMembershipRemovedEvent(CLASSROOM_ID, studentId)) }
    }

    @Test
    fun `가입 정보가 없으면 연결할 수 없다`() {
        every { profileRepository.findByUserId(studentId) } returns null

        assertErrorCode(ErrorCode.STUDENT_PROFILE_REQUIRED) { service.join(command) }
    }

    @Test
    fun `없는 코드는 거절한다`() {
        every { classroomRepository.findByJoinCode(CODE) } returns null

        assertErrorCode(ErrorCode.JOIN_CODE_INVALID) { service.join(command) }
    }

    @Test
    fun `시간이 지난 코드는 거절한다`() {
        givenClassroomWithCode(now().minusSeconds(1))

        assertErrorCode(ErrorCode.JOIN_CODE_EXPIRED) { service.join(command) }
    }

    @Test
    fun `같은 수업에는 반이 달라도 다시 연결할 수 없다`() {
        givenClassroomWithCode(now().plusMinutes(1))
        every { membershipRepository.existsByClassroomCourseIdAndUserId(COURSE_ID, studentId) } returns true

        assertErrorCode(ErrorCode.ALREADY_ENROLLED) { service.join(command) }
    }

    @Test
    fun `다른 교사의 반에서는 학생을 내보낼 수 없다`() {
        every { classroomRepository.findByIdAndCourseUserId(CLASSROOM_ID, teacherId) } returns null

        assertErrorCode(ErrorCode.NOT_FOUND) {
            service.removeStudent(ClassroomMembershipCommand.RemoveStudent(teacherId, CLASSROOM_ID, MEMBERSHIP_ID))
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    private fun givenClassroomWithCode(expiresAt: LocalDateTime) {
        every { classroomRepository.findByJoinCode(CODE) } returns classroom().apply { issueJoinCode(CODE, expiresAt) }
    }

    private fun course(): Course {
        val subject = mockk<Subject> {
            every { id } returns 12L
            every { name } returns "과학"
        }
        return Course.create(school, subject, CourseCommand.Create(teacherId, 1L, 2, 12L))
            .apply { ReflectionTestUtils.setField(this, "id", COURSE_ID) }
    }

    private fun classroom(): Classroom =
        Classroom.create(course(), ClassroomCommand.Create(teacherId, COURSE_ID, 1)).apply {
            ReflectionTestUtils.setField(this, "id", CLASSROOM_ID)
        }

    private fun membership(): ClassroomMembership = ClassroomMembership
        .create(classroom(), ClassroomMembershipCommand.Create(studentId, "홍길동", "20101"))
        .also { ReflectionTestUtils.setField(it, "id", MEMBERSHIP_ID) }

    private fun assertErrorCode(errorCode: ErrorCode, block: () -> Unit) {
        assertThatThrownBy(block).isInstanceOfSatisfying(BusinessException::class.java) {
            assertThat(it.errorCode).isEqualTo(errorCode)
        }
    }

    private companion object {
        const val CODE = "ABC234"
        const val COURSE_ID = 1L
        const val CLASSROOM_ID = 1L
        const val MEMBERSHIP_ID = 10L
    }
}
