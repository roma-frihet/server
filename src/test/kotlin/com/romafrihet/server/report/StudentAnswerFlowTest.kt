package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.AttemptOverrideService
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class StudentAnswerFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var studentAnswerService: StudentAnswerService

    @Autowired
    private lateinit var overrideService: AttemptOverrideService

    @Test
    fun `문제마다 고른 보기와 결과를 내리고 시작하지 않은 문제도 넣는다`() {
        val classroomId = joinedClassroom()
        val first = createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, first, correct = false)

        val lesson = answers(classroomId)

        assertThat(lesson.studentName).isEqualTo("홍길동")
        assertThat(lesson.lessonTitle).isEqualTo("1차시 내용")
        assertThat(lesson.status).isEqualTo(LessonStatus.OPEN)
        assertThat(lesson.questionCount).isEqualTo(2)
        assertThat(lesson.correctCount).isZero()
        assertThat(lesson.answers.map { it.result })
            .containsExactly(StudentAnswerResult.WRONG, StudentAnswerResult.NOT_STARTED)
        val answered = lesson.answers.first()
        val selected = answered.options.single { it.optionId == answered.selectedOptionId }
        assertThat(selected.content).isEqualTo("미토콘드리아")
        assertThat(answered.options.single { it.isCorrect }.content).isEqualTo("엽록체")
        assertThat(answered.attemptAnswerId).isNotNull()
        assertThat(lesson.answers.last().attemptAnswerId).isNull()
    }

    @Test
    fun `교사가 고친 결과와 수정 표시가 반영된다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = false)
        val answerId = requireNotNull(answers(classroomId).answers.single().attemptAnswerId)

        overrideService.override(AttemptOverrideCommand.Update(teacherId, answerId, QuestionResult.RIGHT))

        val after = answers(classroomId)
        assertThat(after.correctCount).isEqualTo(1)
        assertThat(after.answers.single().result).isEqualTo(StudentAnswerResult.RIGHT)
        assertThat(after.answers.single().isOverridden).isTrue()
    }

    @Test
    fun `응시하지 않은 학생은 모든 문제가 NOT_STARTED다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)

        val lesson = answers(classroomId)

        assertThat(lesson.status).isEqualTo(LessonStatus.PLANNED)
        assertThat(lesson.answers.single().result).isEqualTo(StudentAnswerResult.NOT_STARTED)
    }

    @Test
    fun `다른 교사의 반이나 다른 반 학생은 404다`() {
        val classroomId = joinedClassroom(classNo = 1)
        val membershipId = membershipId(classroomId)
        val otherClassroom = createClassroom(classNo = 2)

        assertErrorCode(ErrorCode.NOT_FOUND) {
            studentAnswerService.findLessonAnswers(
                ReportCommand.StudentLessonAnswers(UUID.randomUUID(), classroomId, membershipId, 1),
            )
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            studentAnswerService.findLessonAnswers(
                ReportCommand.StudentLessonAnswers(teacherId, otherClassroom, membershipId, 1),
            )
        }
    }

    private fun membershipId(classroomId: Long): Long =
        requireNotNull(membershipRepository.findAllByClassroomIdIn(listOf(classroomId)).single().id)

    private fun answers(classroomId: Long) = studentAnswerService.findLessonAnswers(
        ReportCommand.StudentLessonAnswers(teacherId, classroomId, membershipId(classroomId), 1),
    )
}
