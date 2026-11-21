package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.AttemptOverrideService
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class StudentDetailFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var overrideService: AttemptOverrideService

    @Test
    fun `학생 상세는 차시별 점수와 최근 차시의 문제별 결과를 내린다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)

        val detail = reportService.findStudentDetail(detailCommand(classroomId))

        assertThat(detail.studentName).isEqualTo("홍길동")
        assertThat(detail.studentNumber).isEqualTo("20101")
        assertThat(detail.scores.single().correctCount).isEqualTo(1)
        assertThat(detail.totalCorrectCount).isEqualTo(1)
        assertThat(detail.latestLessonNumber).isEqualTo(1)
        val answer = detail.latestAnswers.single()
        assertThat(answer.result).isEqualTo(QuestionResult.RIGHT)
        assertThat(answer.isOverridden).isFalse()
        assertThat(answer.questionId).isEqualTo(questionId)
    }

    @Test
    fun `결과를 덮어쓰면 점수가 다시 계산되고 수정 표시가 남는다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = false)
        val before = reportService.findStudentDetail(detailCommand(classroomId))
        assertThat(before.totalCorrectCount).isZero()

        overrideService.override(
            AttemptOverrideCommand.Update(
                teacherId,
                before.latestAnswers.single().attemptAnswerId,
                QuestionResult.RIGHT,
            ),
        )

        val after = reportService.findStudentDetail(detailCommand(classroomId))
        assertThat(after.totalCorrectCount).isEqualTo(1)
        assertThat(after.scores.single().correctCount).isEqualTo(1)
        assertThat(after.latestAnswers.single().isOverridden).isTrue()
    }

    @Test
    fun `덮어쓴 결과는 반 평균과 학생 목록에도 반영된다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = false)
        val answerId = reportService.findStudentDetail(
            detailCommand(classroomId),
        ).latestAnswers.single().attemptAnswerId

        overrideService.override(AttemptOverrideCommand.Update(teacherId, answerId, QuestionResult.RIGHT))

        val students = reportService.findStudents(ReportCommand.Students(teacherId, classroomId, ""))
        assertThat(students.latestAverageCorrect).isEqualTo(1.0)
        assertThat(students.students.single().latestCorrectCount).isEqualTo(1)
    }

    @Test
    fun `다른 교사는 학생 상세를 볼 수 없다`() {
        val classroomId = joinedClassroom()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            reportService.findStudentDetail(
                ReportCommand.StudentDetail(UUID.randomUUID(), classroomId, membershipId(classroomId)),
            )
        }
    }

    @Test
    fun `다른 교사는 결과를 덮어쓸 수 없다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)
        val answerId = reportService.findStudentDetail(
            detailCommand(classroomId),
        ).latestAnswers.single().attemptAnswerId

        assertErrorCode(ErrorCode.NOT_FOUND) {
            overrideService.override(AttemptOverrideCommand.Update(UUID.randomUUID(), answerId, QuestionResult.RIGHT))
        }
    }

    private fun detailCommand(classroomId: Long) =
        ReportCommand.StudentDetail(teacherId, classroomId, membershipId(classroomId))

    private fun membershipId(classroomId: Long): Long =
        requireNotNull(membershipRepository.findAllByClassroomIdOrderByIdAsc(classroomId).single().id)
}
