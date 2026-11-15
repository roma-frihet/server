package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** 시간이 걸린 규칙이라 시계를 고정해 경계를 확인한다. */
class AttemptFinalizerTest {
    private val attemptAnswerRepository = mockk<AttemptAnswerRepository>()
    private val properties = AttemptProperties(graceSeconds = 2)
    private val zone = ZoneId.of("Asia/Seoul")
    private val startedAt = LocalDateTime.of(2026, 11, 15, 10, 0, 0)

    private fun finalizerAt(seconds: Long) = AttemptFinalizer(
        attemptAnswerRepository,
        properties,
        Clock.fixed(startedAt.plusSeconds(seconds).atZone(zone).toInstant(), zone),
    )

    @Test
    fun `마감 직후 유예 시간 안이면 아직 지나지 않은 것으로 본다`() {
        val answer = answer(timeLimitSec = 30)

        // 마감 30초 + 유예 2초 = 32초. 31초는 아직 인정한다.
        assertThat(finalizerAt(31).isOverDeadline(answer, startedAt.plusSeconds(31))).isFalse()
    }

    @Test
    fun `유예 시간을 넘기면 지난 것으로 본다`() {
        val answer = answer(timeLimitSec = 30)

        assertThat(finalizerAt(33).isOverDeadline(answer, startedAt.plusSeconds(33))).isTrue()
    }

    @Test
    fun `임시 저장한 답이 있으면 그 답으로 채점하고 시간 종료로 표시한다`() {
        val attempt = attempt()
        val answer = answer(timeLimitSec = 30, attempt = attempt)
        answer.saveDraft(optionId = CORRECT_OPTION_ID, answerText = null)
        givenExpired(answer)

        finalizerAt(40).finalizeExpired(listOf(attempt), mapOf(LESSON_NUMBER to 2))

        assertThat(answer.finalResult).isEqualTo(QuestionResult.RIGHT)
        assertThat(answer.byTimeout).isTrue()
        assertThat(answer.isFinalized).isTrue()
    }

    @Test
    fun `임시 저장한 답이 없으면 미응답으로 확정한다`() {
        val attempt = attempt()
        val answer = answer(timeLimitSec = 30, attempt = attempt)
        givenExpired(answer)

        finalizerAt(40).finalizeExpired(listOf(attempt), mapOf(LESSON_NUMBER to 2))

        assertThat(answer.finalResult).isEqualTo(QuestionResult.NONE)
        assertThat(answer.byTimeout).isTrue()
    }

    @Test
    fun `확정되면 다음 문제로 넘어가고 마지막이면 응시가 끝난다`() {
        val attempt = attempt()
        val answer = answer(timeLimitSec = 30, attempt = attempt)
        givenExpired(answer)

        finalizerAt(40).finalizeExpired(listOf(attempt), mapOf(LESSON_NUMBER to 1))

        assertThat(attempt.phase).isEqualTo(AttemptPhase.DONE)
        assertThat(attempt.nextIndex).isEqualTo(1)
        assertThat(attempt.completedAt).isNotNull()
    }

    private fun givenExpired(vararg answers: AttemptAnswer) {
        every {
            attemptAnswerRepository.findAllByAttemptIdInAndIsFinalizedFalseAndDeadlineLessThan(any(), any())
        } returns answers.toList()
    }

    private fun attempt(): Attempt = Attempt.create(classroom(), LESSON_NUMBER, UUID.randomUUID()).apply {
        ReflectionTestUtils.setField(this, "id", ATTEMPT_ID)
        startQuestion()
    }

    private fun answer(timeLimitSec: Int, attempt: Attempt = attempt()): AttemptAnswer =
        AttemptAnswer.create(attempt, question(timeLimitSec), 0, startedAt)

    private fun question(timeLimitSec: Int): Question {
        val command = QuestionCommand.Create(
            userId = UUID.randomUUID(),
            courseId = 1L,
            lessonNumber = LESSON_NUMBER,
            type = QuestionType.MULTIPLE_CHOICE,
            content = "문제",
            imageId = null,
            timeLimitSec = timeLimitSec,
            explanation = null,
            options = listOf(QuestionCommand.Option("정답", null, true), QuestionCommand.Option("오답", null, false)),
        )
        return Question.create(course(), 0, command).apply {
            ReflectionTestUtils.setField(options.first(), "id", CORRECT_OPTION_ID)
        }
    }

    private fun course(): Course =
        Course.create(mockk(relaxed = true), mockk(relaxed = true), CourseCommand.Create(UUID.randomUUID(), 1L, 2, 1L))
            .apply { ReflectionTestUtils.setField(this, "id", 1L) }

    private fun classroom(): Classroom = Classroom.create(course(), ClassroomCommand.Create(UUID.randomUUID(), 1L, 1))
        .apply { ReflectionTestUtils.setField(this, "id", 1L) }

    private companion object {
        const val LESSON_NUMBER = 1
        const val ATTEMPT_ID = 1L
        const val CORRECT_OPTION_ID = 10L
    }
}
