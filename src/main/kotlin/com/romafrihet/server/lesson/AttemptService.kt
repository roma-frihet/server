package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.AttemptCommand
import com.romafrihet.server.lesson.dto.AttemptDto
import com.romafrihet.server.lesson.dto.CurrentQuestionDto
import com.romafrihet.server.lesson.dto.FinalizedAnswerDto
import com.romafrihet.server.lesson.dto.LessonResultDto
import com.romafrihet.server.lesson.dto.LessonResultQuestionDto
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 학생이 문제를 하나씩 푼다. 시간은 문제를 시작할 때부터 흐르고 되돌아갈 수 없다.
 * 차시를 닫거나 마감해도 이미 시작한 문제는 제 마감 시각까지 제출을 받고, 새로 시작하는 것만 막는다.
 *
 * 상태를 바꾸는 일은 트랜잭션 안에서 끝내고, 이미지 서명 URL 발급은 트랜잭션 밖에서 한다.
 * 한 반이 동시에 문제를 시작하는 순간에 외부 호출이 커넥션과 락을 붙잡으면 안 된다.
 */
@Service
class AttemptService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val attemptFinalizer: AttemptFinalizer,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun findAttempt(command: AttemptCommand.Query): AttemptDto = inTransaction {
        snapshot(load(command.userId, command.classroomId, command.lessonNumber))
    }.toDto()

    fun start(command: AttemptCommand.Start): AttemptDto = inTransaction {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        assertCheck(lesson.opening.status == LessonStatus.OPEN) { ErrorCode.LESSON_NOT_OPEN }

        val attempt = lesson.attempt ?: attemptRepository.save(
            Attempt.create(lesson.classroom, command.lessonNumber, command.userId),
        )
        assertCheck(attempt.phase != AttemptPhase.DONE) { ErrorCode.ATTEMPT_ALREADY_DONE }
        assertCheck(command.questionIndex == attempt.nextIndex) { ErrorCode.ATTEMPT_OUT_OF_ORDER }

        // 같은 문제로 다시 들어와도 마감 시각을 늘려 주지 않는다.
        if (attempt.phase == AttemptPhase.READY) {
            val question =
                lesson.questions.getOrNull(command.questionIndex) ?: throw BusinessException(ErrorCode.NOT_FOUND)
            attemptAnswerRepository.save(
                AttemptAnswer.create(attempt, question, command.questionIndex, LocalDateTime.now(clock)),
            )
            attempt.startQuestion()
        }
        snapshot(lesson.copy(attempt = attempt))
    }.toDto()

    fun saveDraft(command: AttemptCommand.Answer): AttemptDto = inTransaction {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        val answer = inProgressAnswer(lesson, command.questionIndex)
        assertCheck(!LocalDateTime.now(clock).isAfter(answer.deadline)) { ErrorCode.ATTEMPT_TIME_OVER }
        assertCheck(answer.question.accepts(command.optionId)) { ErrorCode.SUBMISSION_ANSWER_INVALID }
        answer.saveDraft(command.optionId, command.text?.trim())
        snapshot(lesson)
    }.toDto()

    /** 이미 확정된 문제에 다시 제출하면 확정 결과를 그대로 돌려준다. 재전송이 안전해야 한다. */
    fun submit(command: AttemptCommand.Answer): AttemptDto = inTransaction {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        val attempt = lesson.attempt ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        val answer =
            attemptAnswerRepository.findByAttemptIdAndQuestionIndex(requireNotNull(attempt.id), command.questionIndex)
                ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        if (answer.isFinalized) return@inTransaction snapshot(lesson)

        val now = LocalDateTime.now(clock)
        if (attemptFinalizer.isOverDeadline(answer, now)) {
            // 유예 시간까지 지났으면 보낸 답은 무시하고 임시 저장된 답으로 확정한다.
            answer.finalizeWith(answer.optionId, answer.answerText, byTimeout = true, now = now)
        } else {
            assertCheck(answer.question.accepts(command.optionId)) { ErrorCode.SUBMISSION_ANSWER_INVALID }
            val text = command.text?.trim()
            assertCheck(command.optionId != null || !text.isNullOrBlank()) { ErrorCode.ANSWER_REQUIRED }
            answer.finalizeWith(command.optionId, text, byTimeout = false, now = now)
        }
        attempt.completeQuestion(lesson.questions.size, now)
        snapshot(lesson)
    }.toDto()

    /** 마감한 차시만 전체 문제와 정답, 해설을 공개한다. */
    fun findResult(command: AttemptCommand.Query): LessonResultDto {
        val (questions, answers, hasAttempt) = inTransaction {
            val lesson = load(command.userId, command.classroomId, command.lessonNumber)
            assertCheck(lesson.opening.status == LessonStatus.FINISHED) { ErrorCode.NOT_FOUND }
            val byQuestion =
                lesson.attempt?.id
                    ?.let(attemptAnswerRepository::findAllByAttemptIdOrderByQuestionIndexAsc)
                    .orEmpty()
                    .associateBy { requireNotNull(it.question.id) }
            Triple(lesson.questions, byQuestion, lesson.attempt != null)
        }
        val dtos = questionImageUrlResolver.toDtos(questions).associateBy { it.id }
        return LessonResultDto(
            lessonNumber = command.lessonNumber,
            correctCount =
            if (hasAttempt) answers.values.count { it.finalResult == QuestionResult.RIGHT } else null,
            questionCount = questions.size,
            questions =
            questions.map { question ->
                val questionId = requireNotNull(question.id)
                val answer = answers[questionId]
                val correct = question.options.firstOrNull { it.isCorrect }
                LessonResultQuestionDto(
                    question = dtos.getValue(questionId),
                    result = answer?.finalResult ?: QuestionResult.NONE,
                    selectedOptionId = answer?.optionId,
                    answerText = answer?.answerText,
                    correctOptionId = correct?.id,
                    correctAnswer = correct?.content,
                )
            },
        )
    }

    private fun <T> inTransaction(block: () -> T): T = requireNotNull(transactionTemplate.execute { block() })

    /**
     * 반에 연 적 있는 차시인지 확인하고 응시와 문제를 함께 읽는다.
     * 읽기 전에 시간이 지난 응답부터 확정한다. 학생이 돌아오지 않아도 점수가 맞아야 한다.
     */
    private fun load(userId: UUID, classroomId: Long, lessonNumber: Int): LessonAttempt {
        assertCheck(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, userId)) {
            ErrorCode.FORBIDDEN
        }
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, lessonNumber)
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        val classroom =
            classroomRepository.findById(classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
        val questions =
            questionRepository.findAllWithOptions(requireNotNull(classroom.course.id), lessonNumber)
        val attempt =
            attemptRepository.findByClassroomIdAndLessonNumberAndUserId(classroomId, lessonNumber, userId)?.also {
                attemptFinalizer.finalizeExpired(listOf(it), mapOf(lessonNumber to questions.size))
            }
        return LessonAttempt(classroom, opening, questions, attempt)
    }

    private fun inProgressAnswer(lesson: LessonAttempt, questionIndex: Int): AttemptAnswer {
        val attempt = lesson.attempt ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        assertCheck(attempt.phase == AttemptPhase.IN_QUESTION) { ErrorCode.ATTEMPT_NOT_IN_QUESTION }
        val answer =
            attemptAnswerRepository.findByAttemptIdAndQuestionIndex(requireNotNull(attempt.id), questionIndex)
                ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        assertCheck(!answer.isFinalized) { ErrorCode.ATTEMPT_TIME_OVER }
        return answer
    }

    /** 트랜잭션 안에서 필요한 값을 모두 꺼내 둔다. 이미지 URL만 밖에서 붙인다. */
    private fun snapshot(lesson: LessonAttempt): AttemptSnapshot {
        val attempt = lesson.attempt
        val answers =
            attempt?.id?.let(attemptAnswerRepository::findAllByAttemptIdOrderByQuestionIndexAsc).orEmpty()
        val phase = attempt?.phase ?: AttemptPhase.READY
        val nextIndex = attempt?.nextIndex ?: 0
        val finalized = answers.filter { it.isFinalized }
        val current =
            answers
                .firstOrNull { !it.isFinalized && it.questionIndex == nextIndex }
                ?.takeIf { phase == AttemptPhase.IN_QUESTION }
        return AttemptSnapshot(
            phase = phase,
            total = lesson.questions.size,
            nextIndex = nextIndex,
            nextTimeLimitSec = lesson.questions.getOrNull(nextIndex)?.timeLimitSec,
            correctCount =
            if (phase == AttemptPhase.DONE) finalized.count { it.finalResult == QuestionResult.RIGHT } else null,
            currentQuestion = current?.question,
            currentIndex = current?.questionIndex,
            remainingMillis = current?.remainingMillis(LocalDateTime.now(clock)),
            draftOptionId = current?.optionId,
            draftText = current?.answerText,
            last = finalized.maxByOrNull { it.questionIndex }?.let(::toFinalized),
        )
    }

    /** 정답과 해설은 확정된 뒤에만 나간다. */
    private fun toFinalized(answer: AttemptAnswer): FinalizedAnswerDto {
        val correct = answer.question.options.firstOrNull { it.isCorrect }
        return FinalizedAnswerDto(
            questionIndex = answer.questionIndex,
            result = answer.finalResult,
            byTimeout = answer.byTimeout,
            selectedOptionId = answer.optionId,
            answerText = answer.answerText,
            correctOptionId = correct?.id,
            correctAnswer = correct?.content,
            explanation = answer.question.explanation,
        )
    }

    private fun AttemptSnapshot.toDto() = AttemptDto(
        phase = phase,
        total = total,
        nextIndex = nextIndex,
        nextTimeLimitSec = nextTimeLimitSec,
        correctCount = correctCount,
        current =
        currentQuestion?.let {
            CurrentQuestionDto(
                questionIndex = requireNotNull(currentIndex),
                question = questionImageUrlResolver.toDtos(listOf(it)).single(),
                remainingMillis = requireNotNull(remainingMillis),
                draftOptionId = draftOptionId,
                draftText = draftText,
            )
        },
        last = last,
    )

    private data class LessonAttempt(
        val classroom: Classroom,
        val opening: LessonOpening,
        val questions: List<Question>,
        val attempt: Attempt?,
    )

    /** 트랜잭션 안에서 다 꺼낸 값이다. currentQuestion은 options까지 로딩된 상태로만 담는다. */
    private data class AttemptSnapshot(
        val phase: AttemptPhase,
        val total: Int,
        val nextIndex: Int,
        val nextTimeLimitSec: Int?,
        val correctCount: Int?,
        val currentQuestion: Question?,
        val currentIndex: Int?,
        val remainingMillis: Long?,
        val draftOptionId: Long?,
        val draftText: String?,
        val last: FinalizedAnswerDto?,
    )
}
