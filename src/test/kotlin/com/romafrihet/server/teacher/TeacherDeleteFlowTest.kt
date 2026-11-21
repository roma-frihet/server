package com.romafrihet.server.teacher

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.teacher.command.TeacherCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * 차시를 한 번 열면 연 기록이 남아 반과 수업을 영영 지울 수 없던 문제를 막는다.
 * 잘못 만든 반을 되돌릴 방법이 없으면 개발자가 DB에 직접 들어가야 한다.
 */
class TeacherDeleteFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var teacherService: TeacherService

    @Autowired
    private lateinit var courseRepository: CourseRepository

    @Autowired
    private lateinit var lessonRepository: LessonRepository

    @Autowired
    private lateinit var lessonOpeningRepository: LessonOpeningRepository

    @Test
    fun `학생과 기록이 있는 반을 지우면 기록도 함께 사라진다`() {
        val classroomId = joinedClassroom()
        val question = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, question, correct = true)

        teacherService.deleteClassroom(TeacherCommand.DeleteClassroom(teacherId, classroomId))

        assertThat(classroomRepository.findById(classroomId)).isEmpty
        assertThat(membershipRepository.existsByClassroomId(classroomId)).isFalse()
        assertThat(lessonOpeningRepository.existsByClassroomId(classroomId)).isFalse()
        assertThat(attemptRepository.findAllByClassroomIdIn(listOf(classroomId))).isEmpty()
        // 반만 지웠으므로 수업과 문제는 남는다.
        assertThat(questionRepository.findById(question)).isPresent
    }

    @Test
    fun `수업을 지우면 반과 차시와 문제가 모두 사라진다`() {
        val classroomId = joinedClassroom()
        val question = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, question, correct = true)

        teacherService.deleteCourse(TeacherCommand.DeleteCourse(teacherId, courseId))

        assertThat(courseRepository.findById(courseId)).isEmpty
        assertThat(classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId)).isEmpty()
        assertThat(lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId)).isEmpty()
        assertThat(questionRepository.findById(question)).isEmpty
        assertThat(attemptRepository.findAllByClassroomCourseId(courseId)).isEmpty()
    }

    @Test
    fun `남의 수업과 반은 지우지 못한다`() {
        val classroomId = joinedClassroom()
        val other = UUID.randomUUID()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            teacherService.deleteClassroom(TeacherCommand.DeleteClassroom(other, classroomId))
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            teacherService.deleteCourse(TeacherCommand.DeleteCourse(other, courseId))
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            lessonService.delete(LessonCommand.Delete(other, courseId, 1))
        }
    }

    @Test
    fun `연 적만 있고 푼 사람이 없으면 문제를 지울 수 있다`() {
        val classroomId = joinedClassroom()
        val question = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        questionService.delete(QuestionCommand.Delete(teacherId, question))

        assertThat(questionRepository.findById(question)).isEmpty
    }

    @Test
    fun `푼 사람이 있으면 문제와 차시를 지우지 못한다`() {
        val classroomId = joinedClassroom()
        val question = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, question, correct = true)

        assertErrorCode(ErrorCode.LESSON_HAS_ATTEMPTS) {
            questionService.delete(QuestionCommand.Delete(teacherId, question))
        }
        assertErrorCode(ErrorCode.LESSON_HAS_ATTEMPTS) {
            lessonService.delete(LessonCommand.Delete(teacherId, courseId, 1))
        }
    }

    @Test
    fun `차시를 지우면 그 차시의 문제와 연 기록만 사라진다`() {
        val classroomId = joinedClassroom()
        val first = createQuestion(lessonNumber = 1)
        val second = createQuestion(lessonNumber = 2)
        openLesson(classroomId, 1)

        lessonService.delete(LessonCommand.Delete(teacherId, courseId, 1))

        assertThat(lessonRepository.findByCourseIdAndLessonNumber(courseId, 1)).isNull()
        assertThat(questionRepository.findById(first)).isEmpty
        // 다른 차시는 건드리지 않는다. 번호도 당겨지지 않는다.
        assertThat(lessonRepository.findByCourseIdAndLessonNumber(courseId, 2)).isNotNull()
        assertThat(questionRepository.findById(second)).isPresent
        assertThat(lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, 1)).isNull()
    }
}
