package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.rqrs.StudentReportResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class StudentReportFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var studentReportService: StudentReportService

    @Test
    fun `리포트는 차시별 내 점수와 반 평균을 함께 내린다`() {
        val classroomId = joinedClassroom()
        val first = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, first, correct = true)

        val report = studentReportService.findReport(command(classroomId))

        assertThat(report.classNo).isEqualTo(1)
        assertThat(report.subjectName).isEqualTo("과학")
        assertThat(report.scores.single().correctCount).isEqualTo(1)
        assertThat(report.scores.single().classAverageCorrect).isEqualTo(1.0)
        assertThat(report.latestScore?.lessonNumber).isEqualTo(1)
        assertThat(report.totalCorrectCount).isEqualTo(1)
    }

    @Test
    fun `지금 열린 차시와 내 응시 상태를 알려 준다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        val open = requireNotNull(studentReportService.findReport(command(classroomId)).openLesson)

        assertThat(open.lessonNumber).isEqualTo(1)
        assertThat(open.questionCount).isEqualTo(1)
        assertThat(open.totalTimeLimitSec).isEqualTo(30)
        assertThat(open.phase).isEqualTo(AttemptPhase.READY)
        assertThat(open.nextIndex).isZero()
    }

    @Test
    fun `마감한 차시에서 맞히지 못한 문제를 최근 차시부터 모아 준다`() {
        val classroomId = joinedClassroom()
        val wrong = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, wrong, correct = false)
        openingService.finish(change(classroomId, 1))

        val questions = studentReportService.findWrongQuestions(command(classroomId))

        val only = questions.single()
        assertThat(only.lessonNumber).isEqualTo(1)
        assertThat(only.result).isEqualTo(QuestionResult.WRONG)
        assertThat(only.correctAnswer).isEqualTo("엽록체")
        assertThat(only.questionId).isEqualTo(wrong)
    }

    /** 마감 전에는 정답을 공개하지 않으므로 틀린 문제도 내리지 않는다. */
    @Test
    fun `마감하지 않은 차시의 틀린 문제는 내리지 않는다`() {
        val classroomId = joinedClassroom()
        val wrong = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, wrong, correct = false)

        assertThat(studentReportService.findWrongQuestions(command(classroomId))).isEmpty()
    }

    /** 반 평균은 주되 다른 학생이 누군지, 몇 점인지는 알려주지 않는다. */
    @Test
    fun `리포트에 다른 학생의 이름과 점수가 담기지 않는다`() {
        val classroomId = joinedClassroom()
        val other = UUID.randomUUID()
        request(classroomId, other, "김영희", "20102")
        val question = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, question, correct = false)
        submit(classroomId, other, question, correct = true)

        val report = studentReportService.findReport(command(classroomId))
        val body = StudentReportResponse.Result.of(report).toString()

        assertThat(report.scores.single().correctCount).isZero()
        assertThat(report.scores.single().classAverageCorrect).isEqualTo(0.5)
        assertThat(body).doesNotContain("김영희", "20102")
        assertThat(body).doesNotContain(other.toString())
    }

    @Test
    fun `연결되지 않은 반의 리포트는 볼 수 없다`() {
        val classroomId = joinedClassroom()

        assertErrorCode(ErrorCode.FORBIDDEN) {
            studentReportService.findReport(ReportCommand.StudentReport(UUID.randomUUID(), classroomId))
        }
    }

    private fun command(classroomId: Long) = ReportCommand.StudentReport(studentId, classroomId)

    private fun change(classroomId: Long, lessonNumber: Int) =
        com.romafrihet.server.lesson.command.LessonOpeningCommand.Change(teacherId, classroomId, lessonNumber)
}
