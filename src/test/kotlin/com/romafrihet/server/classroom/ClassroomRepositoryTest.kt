package com.romafrihet.server.classroom

import com.romafrihet.server.TestcontainersConfiguration
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.config.JpaConfig
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.subject.SchoolLevel
import com.romafrihet.server.subject.SubjectRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/** 반 번호는 정렬과 유일성이 DB에 걸려 있으므로 실제 스키마에서 확인한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class ClassroomRepositoryTest(
    @Autowired private val classroomRepository: ClassroomRepository,
    @Autowired private val courseRepository: CourseRepository,
    @Autowired private val schoolRepository: SchoolRepository,
    @Autowired private val subjectRepository: SubjectRepository,
) {
    private val teacherId = UUID.randomUUID()

    /** 이름이 문자열이던 때는 "1반, 10반, 2반" 순으로 섞였다. */
    @Test
    fun `반 목록은 번호 순으로 내려간다`() {
        val courseId = requireNotNull(saveCourse(grade = 2).id)
        listOf(10, 2, 1, 20).forEach { saveClassroom(courseId, it) }

        val numbers = classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId).map { it.classNo }

        assertThat(numbers).containsExactly(1, 2, 10, 20)
    }

    @Test
    fun `수업이 다르면 같은 번호의 반을 쓸 수 있다`() {
        val science = requireNotNull(saveCourse(grade = 2).id)
        val math = requireNotNull(saveCourse(grade = 3).id)

        saveClassroom(science, 1)
        saveClassroom(math, 1)

        assertThat(classroomRepository.findAllByCourseIdOrderByClassNoAsc(science)).hasSize(1)
        assertThat(classroomRepository.findAllByCourseIdOrderByClassNoAsc(math)).hasSize(1)
    }

    @Test
    fun `내 반 목록은 수업과 번호 순으로 내려가고 다른 교사 반은 섞이지 않는다`() {
        val courseId = requireNotNull(saveCourse(grade = 2).id)
        listOf(3, 1).forEach { saveClassroom(courseId, it) }
        saveClassroom(requireNotNull(saveCourse(grade = 3, userId = UUID.randomUUID()).id), 1)

        val mine = classroomRepository.findAllByCourseUserIdOrderByCourseIdAscClassNoAsc(teacherId)

        assertThat(mine.map { it.classNo }).containsExactly(1, 3)
    }

    @Test
    fun `같은 수업에 같은 번호의 반은 저장할 수 없다`() {
        val courseId = requireNotNull(saveCourse(grade = 2).id)
        saveClassroom(courseId, 1)

        assertThatThrownBy { saveClassroom(courseId, 1) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun saveCourse(grade: Int, userId: UUID = teacherId): Course {
        val school = schoolRepository.findAll().first()
        val subject = subjectRepository
            .findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE)
            .single { it.name == "과학" }
        val command = CourseCommand.Create(userId, requireNotNull(school.id), grade, requireNotNull(subject.id))
        return courseRepository.saveAndFlush(Course.create(school, subject, command))
    }

    private fun saveClassroom(courseId: Long, classNo: Int): Classroom {
        val course = courseRepository.getReferenceById(courseId)
        val command = ClassroomCommand.Create(teacherId, courseId, classNo)
        return classroomRepository.saveAndFlush(Classroom.create(course, command))
    }
}
