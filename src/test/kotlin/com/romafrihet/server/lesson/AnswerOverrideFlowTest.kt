package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class AnswerOverrideFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var overrideService: AttemptOverrideService

    @Autowired
    private lateinit var resultOverrideRepository: ResultOverrideRepository

    @Test
    fun `공백과 대소문자만 다른 같은 답을 한 번에 인정하고 이력을 남긴다`() {
        val classroomId = createClassroom()
        val questionId = createShortAnswer()
        openLesson(classroomId, 1)
        val first = answerText(classroomId, questionId, "승화현상")
        val second = answerText(classroomId, questionId, "승화 현상")
        val other = answerText(classroomId, questionId, "기화")

        val changed = overrideService.overrideByAnswer(command(classroomId, questionId, "승화현상"))

        assertThat(changed).isEqualTo(2)
        assertThat(resultOf(first)).isEqualTo(QuestionResult.RIGHT)
        assertThat(resultOf(second)).isEqualTo(QuestionResult.RIGHT)
        assertThat(resultOf(other)).isEqualTo(QuestionResult.WRONG)
        assertThat(attemptAnswerRepository.findById(first).orElseThrow().isOverridden).isTrue()
        assertThat(resultOverrideRepository.findAll().count { it.attemptAnswer.id in listOf(first, second) })
            .isEqualTo(2)
    }

    @Test
    fun `이미 그 결과인 응답은 건너뛰고 WRONG으로 다시 취소할 수 있다`() {
        val classroomId = createClassroom()
        val questionId = createShortAnswer()
        openLesson(classroomId, 1)
        val answerId = answerText(classroomId, questionId, "승화현상")
        overrideService.overrideByAnswer(command(classroomId, questionId, "승화현상"))

        val again = overrideService.overrideByAnswer(command(classroomId, questionId, "승화현상"))
        val reverted =
            overrideService.overrideByAnswer(command(classroomId, questionId, "승화현상", QuestionResult.WRONG))

        assertThat(again).isZero()
        assertThat(reverted).isEqualTo(1)
        assertThat(resultOf(answerId)).isEqualTo(QuestionResult.WRONG)
    }

    @Test
    fun `다른 반의 같은 문제 응답은 바뀌지 않는다`() {
        val classroomId = createClassroom(classNo = 1)
        val otherClassroom = createClassroom(classNo = 2)
        val questionId = createShortAnswer()
        openLesson(classroomId, 1)
        openLesson(otherClassroom, 1)
        val other = answerText(otherClassroom, questionId, "승화현상")

        overrideService.overrideByAnswer(command(classroomId, questionId, "승화현상"))

        assertThat(resultOf(other)).isEqualTo(QuestionResult.WRONG)
    }

    @Test
    fun `객관식이면 거절하고 다른 교사나 다른 차시면 404다`() {
        val classroomId = createClassroom()
        val choiceId = createQuestion(lessonNumber = 1)
        val questionId = createShortAnswer()

        assertErrorCode(ErrorCode.QUESTION_NOT_SHORT_ANSWER) {
            overrideService.overrideByAnswer(command(classroomId, choiceId, "엽록체"))
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            overrideService.overrideByAnswer(
                command(classroomId, questionId, "승화").copy(userId = UUID.randomUUID()),
            )
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            overrideService.overrideByAnswer(command(classroomId, questionId, "승화").copy(lessonNumber = 2))
        }
    }

    private fun command(
        classroomId: Long,
        questionId: Long,
        answerText: String,
        result: QuestionResult = QuestionResult.RIGHT,
    ) = AttemptOverrideCommand.ByAnswer(teacherId, classroomId, 1, questionId, answerText, result)

    private fun resultOf(answerId: Long): QuestionResult =
        attemptAnswerRepository.findById(answerId).orElseThrow().finalResult

    private fun createShortAnswer(): Long = questionService.create(
        QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = 1,
            type = QuestionType.SHORT_ANSWER,
            content = "드라이아이스가 작아지는 현상은?",
            imageId = null,
            timeLimitSec = 30,
            explanation = null,
            options = listOf(choice("승화", correct = true)),
        ),
    )

    /** 학생 응시 API 없이 주관식 답을 확정하고 응답 id를 돌려준다. */
    private fun answerText(classroomId: Long, questionId: Long, text: String): Long {
        val question = requireNotNull(questionRepository.findWithOptions(questionId, teacherId))
        val classroom = classroomRepository.findById(classroomId).orElseThrow()
        val attempt = attemptRepository.save(Attempt.create(classroom, question.lessonNumber, UUID.randomUUID()))
        val now = LocalDateTime.now()
        val answer = AttemptAnswer.create(attempt, question, 0, now)
        answer.finalizeWith(null, text, byTimeout = false, now = now)
        return requireNotNull(attemptAnswerRepository.save(answer).id)
    }
}
