package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.lesson.Attempt
import com.romafrihet.server.lesson.AttemptAnswer
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * 학생이 문제를 열어 두고 돌아오지 않아도 교사 화면의 점수는 맞아야 한다.
 * 교사가 화면을 열 때 시간이 지난 응답이 확정되는지 본다.
 */
class TeacherReportFinalizationTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var reportService: ReportService

    @Test
    fun `교사가 학생 목록을 열면 시간이 지난 응답이 확정된다`() {
        val abandoned = abandonedQuestion()

        reportService.findStudents(ReportCommand.Students(teacherId, abandoned.classroomId, ""))

        assertThat(isFinalized(abandoned.answerId)).isTrue()
    }

    @Test
    fun `교사가 점수 목록을 열면 시간이 지난 응답이 확정된다`() {
        val abandoned = abandonedQuestion()

        reportService.findScores(ReportCommand.Scores(teacherId, courseId, null, null, 0, 20))

        assertThat(isFinalized(abandoned.answerId)).isTrue()
    }

    @Test
    fun `교사가 대시보드를 열면 시간이 지난 응답이 확정된다`() {
        val abandoned = abandonedQuestion()

        reportService.findDashboard(ReportCommand.Dashboard(teacherId, courseId))

        assertThat(isFinalized(abandoned.answerId)).isTrue()
    }

    private fun isFinalized(answerId: Long): Boolean =
        attemptAnswerRepository.findById(answerId).orElseThrow().isFinalized

    /**
     * 학생이 문제를 시작만 하고 제한 시간을 넘긴 상태를 만든다.
     * 테스트 사이에 롤백이 없으므로 반과 차시를 테스트마다 다르게 쓴다.
     * 한 번 연 차시에는 문제를 더할 수 없어서 차시를 돌려쓸 수도 없다.
     */
    private fun abandonedQuestion(): AbandonedQuestion {
        val lessonNumber = lessonNumbers.incrementAndGet()
        val classroomId = joinedClassroom(classNo = lessonNumber)
        val questionId = createQuestion(lessonNumber = lessonNumber)
        openLesson(classroomId, lessonNumber)
        val question = requireNotNull(questionRepository.findWithOptions(questionId, teacherId))
        val classroom = classroomRepository.findById(classroomId).orElseThrow()
        val attempt = attemptRepository.save(Attempt.create(classroom, lessonNumber, studentId))
        val answer =
            attemptAnswerRepository.save(
                AttemptAnswer.create(attempt, question, 0, LocalDateTime.now().minusHours(1)),
            )
        attempt.startQuestion()
        attemptRepository.save(attempt)
        return AbandonedQuestion(classroomId, requireNotNull(answer.id))
    }

    private data class AbandonedQuestion(val classroomId: Long, val answerId: Long)

    private companion object {
        val lessonNumbers = AtomicInteger(0)
    }
}
