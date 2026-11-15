package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.School
import com.romafrihet.server.subject.Subject
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ClassroomServiceTest {
    private val classroomRepository = mockk<ClassroomRepository>()
    private val courseRepository = mockk<CourseRepository>()
    private val membershipRepository = mockk<ClassroomMembershipRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-11-15T01:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service =
        ClassroomService(classroomRepository, courseRepository, membershipRepository, JoinCodeGenerator(), clock)

    private val teacherId = UUID.randomUUID()

    @Test
    fun `반 목록은 수업의 학년과 과목을 함께 담아 돌려준다`() {
        every { classroomRepository.findAllByCourseUserIdOrderByCourseIdAscClassNoAsc(teacherId) } returns
            listOf(classroom(classNo = 3))
        every { membershipRepository.findStudentCounts(listOf(CLASSROOM_ID)) } returns
            emptyList()

        val result = service.findAll(teacherId).single()

        assertThat(result.classNo).isEqualTo(3)
        assertThat(result.grade).isEqualTo(2)
        assertThat(result.subjectName).isEqualTo("과학")
        assertThat(result.studentCount).isZero()
    }

    @Test
    fun `합류 코드는 6자리로 발급하고 5분 뒤 만료된다`() {
        every { classroomRepository.findByIdAndCourseUserId(CLASSROOM_ID, teacherId) } returns classroom()
        every { membershipRepository.findStudentCounts(listOf(CLASSROOM_ID)) } returns
            emptyList()

        val result = service.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, CLASSROOM_ID))

        assertThat(result.joinCode).hasSize(6)
        assertThat(result.joinCodeExpiresAt).isEqualTo(LocalDateTime.now(clock).plusMinutes(5))
    }

    @Test
    fun `같은 수업에 같은 번호의 반이 있으면 거절한다`() {
        every { courseRepository.findByIdAndUserId(COURSE_ID, teacherId) } returns course()
        every { classroomRepository.existsByCourseIdAndClassNo(COURSE_ID, 1) } returns true

        assertThatThrownBy { service.create(ClassroomCommand.Create(teacherId, COURSE_ID, 1)) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CLASSROOM_DUPLICATED)
    }

    @Test
    fun `다른 교사의 수업에는 반을 만들 수 없다`() {
        every { courseRepository.findByIdAndUserId(COURSE_ID, teacherId) } returns null

        assertThatThrownBy { service.create(ClassroomCommand.Create(teacherId, COURSE_ID, 1)) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND)
    }

    private fun course(): Course {
        val school = mockk<School> {
            every { id } returns 1L
            every { name } returns "다원중학교"
        }
        val subject = mockk<Subject> {
            every { id } returns SUBJECT_ID
            every { name } returns "과학"
        }
        val command = CourseCommand.Create(teacherId, 1L, 2, SUBJECT_ID)
        return Course.create(school, subject, command).apply { ReflectionTestUtils.setField(this, "id", COURSE_ID) }
    }

    private fun classroom(classNo: Int = 1): Classroom =
        Classroom.create(course(), ClassroomCommand.Create(teacherId, COURSE_ID, classNo)).apply {
            ReflectionTestUtils.setField(this, "id", CLASSROOM_ID)
        }

    private companion object {
        const val COURSE_ID = 1L
        const val CLASSROOM_ID = 1L
        const val SUBJECT_ID = 12L
    }
}
