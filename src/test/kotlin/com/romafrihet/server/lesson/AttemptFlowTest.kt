package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.AttemptCommand
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionSynchronizationManager

/** 시간이 걸린 규칙은 고정 시계를 쓸 수 없는 통합 경로라, 시간이 흐르지 않는 조건만 여기서 본다. */
class AttemptFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var attemptService: AttemptService

    /** 한 반이 동시에 문제를 시작할 때 외부 호출이 커넥션과 락을 붙잡으면 안 된다. */
    @Test
    fun `이미지 서명 URL은 트랜잭션 밖에서 발급한다`() {
        val classroomId = openedClassroom()
        var activeDuringCall: Boolean? = null
        every { storageClient.createSignedUrls(any()) } answers {
            activeDuringCall = TransactionSynchronizationManager.isActualTransactionActive()
            emptyMap()
        }

        attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))

        assertThat(activeDuringCall).isFalse()
    }

    @Test
    fun `응시를 시작하기 전에는 아직 아무것도 풀지 않은 상태다`() {
        val classroomId = openedClassroom()

        val attempt = attemptService.findAttempt(AttemptCommand.Query(studentId, classroomId, 1))

        assertThat(attempt.phase).isEqualTo(AttemptPhase.READY)
        assertThat(attempt.total).isEqualTo(2)
        assertThat(attempt.nextIndex).isZero()
        assertThat(attempt.current).isNull()
        assertThat(attempt.last).isNull()
    }

    @Test
    fun `문제를 시작하면 본문과 남은 시간을 받고 정답은 받지 않는다`() {
        val classroomId = openedClassroom()

        val attempt = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))

        val current = requireNotNull(attempt.current)
        assertThat(attempt.phase).isEqualTo(AttemptPhase.IN_QUESTION)
        assertThat(current.question.content).isEqualTo("광합성이 일어나는 곳은?")
        assertThat(current.remainingMillis).isPositive()
        assertThat(attempt.last).isNull()
    }

    @Test
    fun `같은 문제를 다시 시작해도 마감 시각이 늘어나지 않는다`() {
        val classroomId = openedClassroom()
        val first = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))

        val second = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))

        assertThat(second.current?.remainingMillis)
            .isLessThanOrEqualTo(requireNotNull(first.current).remainingMillis)
        assertThat(second.nextIndex).isZero()
    }

    @Test
    fun `임시 저장한 답은 다시 조회해도 남아 있다`() {
        val classroomId = openedClassroom()
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        val optionId = requireNotNull(started.current).question.options.first().id

        attemptService.saveDraft(AttemptCommand.Answer(studentId, classroomId, 1, 0, optionId, null))

        val attempt = attemptService.findAttempt(AttemptCommand.Query(studentId, classroomId, 1))
        assertThat(attempt.current?.draftOptionId).isEqualTo(optionId)
    }

    @Test
    fun `제출하면 결과와 정답과 해설을 받고 다음 문제로 넘어간다`() {
        val classroomId = openedClassroom()
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        val correctId = requireNotNull(started.current).question.options.single { it.isCorrect }.id

        val attempt =
            attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, 0, correctId, null))

        val last = requireNotNull(attempt.last)
        assertThat(last.result).isEqualTo(QuestionResult.RIGHT)
        assertThat(last.byTimeout).isFalse()
        assertThat(last.explanation).isEqualTo("잎의 엽록체에서 일어납니다.")
        assertThat(attempt.phase).isEqualTo(AttemptPhase.READY)
        assertThat(attempt.nextIndex).isEqualTo(1)
    }

    @Test
    fun `마지막 문제까지 풀면 끝나고 맞힌 개수를 알려 준다`() {
        val classroomId = openedClassroom()
        solve(classroomId, index = 0, correct = true)
        solve(classroomId, index = 1, correct = false)

        val attempt = attemptService.findAttempt(AttemptCommand.Query(studentId, classroomId, 1))

        assertThat(attempt.phase).isEqualTo(AttemptPhase.DONE)
        assertThat(attempt.correctCount).isEqualTo(1)
    }

    @Test
    fun `차시를 닫아도 이미 시작한 문제는 제출할 수 있다`() {
        val classroomId = openedClassroom()
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        val correctId = requireNotNull(started.current).question.options.single { it.isCorrect }.id
        openingService.close(
            com.romafrihet.server.lesson.command.LessonOpeningCommand.Change(teacherId, classroomId, 1),
        )

        val attempt =
            attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, 0, correctId, null))

        assertThat(attempt.last?.result).isEqualTo(QuestionResult.RIGHT)
    }

    @Test
    fun `차시를 닫으면 새 문제는 시작할 수 없다`() {
        val classroomId = openedClassroom()
        openingService.close(
            com.romafrihet.server.lesson.command.LessonOpeningCommand.Change(teacherId, classroomId, 1),
        )

        assertErrorCode(ErrorCode.LESSON_NOT_OPEN) {
            attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        }
    }

    @Test
    fun `앞 문제를 건너뛰고 시작할 수 없다`() {
        val classroomId = openedClassroom()

        assertErrorCode(ErrorCode.ATTEMPT_OUT_OF_ORDER) {
            attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 1))
        }
    }

    @Test
    fun `시작하지 않은 문제에는 제출할 수 없다`() {
        val classroomId = openedClassroom()

        assertErrorCode(ErrorCode.ATTEMPT_NOT_IN_QUESTION) {
            attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, 0, null, "답"))
        }
    }

    @Test
    fun `빈 답은 제출할 수 없다`() {
        val classroomId = openedClassroom()
        attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))

        assertErrorCode(ErrorCode.ANSWER_REQUIRED) {
            attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, 0, null, "  "))
        }
    }

    @Test
    fun `다 푼 차시는 다시 시작할 수 없다`() {
        val classroomId = openedClassroom()
        solve(classroomId, index = 0, correct = true)
        solve(classroomId, index = 1, correct = true)

        assertErrorCode(ErrorCode.ATTEMPT_ALREADY_DONE) {
            attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 2))
        }
    }

    @Test
    fun `같은 문제에 두 번 제출해도 첫 결과가 유지된다`() {
        val classroomId = openedClassroom()
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        val options = requireNotNull(started.current).question.options
        attemptService.submit(
            AttemptCommand.Answer(studentId, classroomId, 1, 0, options.single { it.isCorrect }.id, null),
        )

        val again = attemptService.submit(
            AttemptCommand.Answer(studentId, classroomId, 1, 0, options.single { !it.isCorrect }.id, null),
        )

        assertThat(again.last?.result).isEqualTo(QuestionResult.RIGHT)
        assertThat(again.nextIndex).isEqualTo(1)
    }

    @Test
    fun `연결되지 않은 반의 차시는 풀 수 없다`() {
        val classroomId = openedClassroom()

        assertErrorCode(ErrorCode.FORBIDDEN) {
            attemptService.findAttempt(AttemptCommand.Query(java.util.UUID.randomUUID(), classroomId, 1))
        }
    }

    private fun solve(classroomId: Long, index: Int, correct: Boolean) {
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, index))
        val question = requireNotNull(started.current).question
        val optionId =
            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                question.options.single { it.isCorrect == correct }.id
            } else {
                null
            }
        val text = if (question.type == QuestionType.SHORT_ANSWER) (if (correct) "이산화탄소" else "산소") else null
        attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, index, optionId, text))
    }

    /** 2문제짜리 차시를 열고 학생을 연결한다. */
    private fun openedClassroom(): Long {
        val classroomId = joinedClassroom()
        createExplainedQuestion()
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        return classroomId
    }

    private fun createExplainedQuestion() = questionService.create(
        com.romafrihet.server.lesson.command.QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = 1,
            type = QuestionType.MULTIPLE_CHOICE,
            content = "광합성이 일어나는 곳은?",
            imageId = null,
            timeLimitSec = 300,
            explanation = "잎의 엽록체에서 일어납니다.",
            options = listOf(choice("엽록체", correct = true), choice("미토콘드리아")),
        ),
    )
}
