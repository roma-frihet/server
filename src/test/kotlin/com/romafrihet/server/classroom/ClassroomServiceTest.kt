package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.command.CourseCommand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class ClassroomServiceTest {
    private val classroomRepository = mockk<ClassroomRepository>()
    private val membershipRepository = mockk<ClassroomMembershipRepository>()
    private val service =
        ClassroomService(classroomRepository, mockk<CourseRepository>(), membershipRepository, JoinCodeGenerator())

    private val teacherId = UUID.randomUUID()

    @Test
    fun `합류 코드는 6자리로 발급하고 3분 뒤 만료된다`() {
        every { classroomRepository.findByIdAndCourseUserId(1L, teacherId) } returns classroom()
        every { membershipRepository.findStudentCounts(listOf(1L), MembershipStatus.APPROVED) } returns emptyList()

        val result = service.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, 1L))

        assertThat(result.joinCode).hasSize(6)
        assertThat(result.joinCodeExpiresAt)
            .isCloseTo(LocalDateTime.now().plusMinutes(3), within(5, ChronoUnit.SECONDS))
    }

    private fun classroom(): Classroom {
        val course = Course.create(mockk(relaxed = true), CourseCommand.Create(teacherId, 1L, 2, "과학"))
        ReflectionTestUtils.setField(course, "id", 1L)
        return Classroom.create(course, ClassroomCommand.Create(teacherId, 1L, "1반")).apply {
            ReflectionTestUtils.setField(this, "id", 1L)
        }
    }
}
