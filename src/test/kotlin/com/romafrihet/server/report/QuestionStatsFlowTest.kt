package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.Attempt
import com.romafrihet.server.lesson.AttemptAnswer
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class QuestionStatsFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var questionStatsService: QuestionStatsService

    @Test
    fun `객관식은 보기별 응답 수와 정답 수를 센다`() {
        val classroomId = createClassroom()
        val second = UUID.randomUUID()
        request(classroomId, studentId, "홍길동", "20101")
        request(classroomId, second, "김철수", "20102")
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)
        submit(classroomId, second, questionId, correct = false)

        val stats = stats(lessonNumber = 1).single()

        assertThat(stats.status).isEqualTo(LessonStatus.OPEN)
        assertThat(stats.studentCount).isEqualTo(2)
        assertThat(stats.responseCount).isEqualTo(2)
        val question = stats.questions.single()
        assertThat(question.answeredCount).isEqualTo(2)
        assertThat(question.rightCount).isEqualTo(1)
        assertThat(question.options.map { it.content to it.count })
            .containsExactly("엽록체" to 1, "미토콘드리아" to 1)
        assertThat(question.options.first().isCorrect).isTrue()
    }

    @Test
    fun `주관식은 공백과 대소문자를 무시하고 같은 답끼리 묶는다`() {
        val classroomId = createClassroom()
        val questionId = createShortAnswer(lessonNumber = 1, accepted = "승화")
        openLesson(classroomId, 1)
        answerText(classroomId, UUID.randomUUID(), questionId, "승화")
        answerText(classroomId, UUID.randomUUID(), questionId, "기화")
        answerText(classroomId, UUID.randomUUID(), questionId, "기 화")
        answerText(classroomId, UUID.randomUUID(), questionId, null)

        val question = stats(lessonNumber = 1).single().questions.single()

        assertThat(question.acceptedAnswers).containsExactly("승화")
        assertThat(question.answeredCount).isEqualTo(3)
        assertThat(question.rightCount).isEqualTo(1)
        assertThat(question.noAnswerCount).isEqualTo(1)
        assertThat(question.answerTexts.map { Triple(it.answerText, it.count, it.rightCount) })
            .containsExactly(Triple("기화", 2, 0), Triple("승화", 1, 1))
        assertThat(question.options).isEmpty()
    }

    @Test
    fun `반을 여러 개 주면 반마다 나란히 내리고 열지 않은 반은 PLANNED다`() {
        val first = joinedClassroom(classNo = 1)
        val second = createClassroom(classNo = 2)
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(first, 1)
        submit(first, studentId, questionId, correct = true)

        val stats = stats(lessonNumber = 1, classroomIds = listOf(second, first))

        assertThat(stats.map { it.classNo }).containsExactly(1, 2)
        assertThat(stats.last().status).isEqualTo(LessonStatus.PLANNED)
        assertThat(stats.last().questions.single().answeredCount).isZero()
    }

    @Test
    fun `다른 교사의 수업이나 없는 차시는 404다`() {
        createClassroom()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            questionStatsService.findQuestionStats(
                ReportCommand.QuestionStats(UUID.randomUUID(), courseId, 1, emptyList()),
            )
        }
        assertErrorCode(ErrorCode.NOT_FOUND) { stats(lessonNumber = LESSON_COUNT + 1) }
    }

    private fun stats(lessonNumber: Int, classroomIds: List<Long> = emptyList()) = questionStatsService
        .findQuestionStats(ReportCommand.QuestionStats(teacherId, courseId, lessonNumber, classroomIds))

    private fun createShortAnswer(lessonNumber: Int, accepted: String): Long = questionService.create(
        QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = QuestionType.SHORT_ANSWER,
            content = "드라이아이스가 작아지는 현상은?",
            imageId = null,
            timeLimitSec = 30,
            explanation = null,
            options = listOf(choice(accepted, correct = true)),
        ),
    )

    /** 학생 응시 API 없이 주관식 답을 확정한다. text가 null이면 시간이 지나 미응답으로 확정된 것이다. */
    private fun answerText(classroomId: Long, userId: UUID, questionId: Long, text: String?) {
        val question = requireNotNull(questionRepository.findWithOptions(questionId, teacherId))
        val classroom = classroomRepository.findById(classroomId).orElseThrow()
        val attempt = attemptRepository.save(Attempt.create(classroom, question.lessonNumber, userId))
        val now = LocalDateTime.now()
        val answer = AttemptAnswer.create(attempt, question, 0, now)
        answer.finalizeWith(null, text, byTimeout = text == null, now = now)
        attemptAnswerRepository.save(answer)
    }
}
