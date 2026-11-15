package com.romafrihet.server.course

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.School
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.subject.Subject
import com.romafrihet.server.subject.SubjectRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional
import java.util.UUID

class CourseServiceTest {
    private val courseRepository = mockk<CourseRepository>()
    private val schoolRepository = mockk<SchoolRepository>()
    private val subjectRepository = mockk<SubjectRepository>()
    private val service = CourseService(courseRepository, schoolRepository, subjectRepository)

    private val teacherId = UUID.randomUUID()

    private val school = mockk<School> {
        every { id } returns SCHOOL_ID
        every { name } returns "다원중학교"
    }
    private val subject = mockk<Subject> {
        every { id } returns SUBJECT_ID
        every { name } returns "과학"
    }

    @Test
    fun `수업은 과목 id로 만들고 과목 이름을 함께 돌려준다`() {
        givenNoDuplicate()
        every { schoolRepository.findById(SCHOOL_ID) } returns Optional.of(school)
        every { subjectRepository.findById(SUBJECT_ID) } returns Optional.of(subject)
        every { courseRepository.save(any()) } answers {
            firstArg<Course>().apply { ReflectionTestUtils.setField(this, "id", COURSE_ID) }
        }

        val result = service.create(command())

        assertThat(result.subjectId).isEqualTo(SUBJECT_ID)
        assertThat(result.subjectName).isEqualTo("과학")
        assertThat(result.grade).isEqualTo(2)
        assertThat(result.id).isEqualTo(COURSE_ID)
    }

    @Test
    fun `같은 학년의 같은 과목이 이미 있으면 거절한다`() {
        every { courseRepository.existsByUserIdAndGradeAndSubjectId(teacherId, 2, SUBJECT_ID) } returns true

        assertThatThrownBy { service.create(command()) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.COURSE_DUPLICATED)
    }

    @Test
    fun `없는 과목으로는 수업을 만들 수 없다`() {
        givenNoDuplicate()
        every { schoolRepository.findById(SCHOOL_ID) } returns Optional.of(school)
        every { subjectRepository.findById(SUBJECT_ID) } returns Optional.empty()

        assertThatThrownBy { service.create(command()) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND)
    }

    @Test
    fun `없는 학교로는 수업을 만들 수 없다`() {
        givenNoDuplicate()
        every { schoolRepository.findById(SCHOOL_ID) } returns Optional.empty()

        assertThatThrownBy { service.create(command()) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND)
    }

    private fun givenNoDuplicate() {
        every { courseRepository.existsByUserIdAndGradeAndSubjectId(teacherId, 2, SUBJECT_ID) } returns false
    }

    private fun command() = CourseCommand.Create(teacherId, SCHOOL_ID, 2, SUBJECT_ID)

    private companion object {
        const val COURSE_ID = 7L
        const val SCHOOL_ID = 1L
        const val SUBJECT_ID = 12L
    }
}
