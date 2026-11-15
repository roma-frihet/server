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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 학생이 문제를 하나씩 푼다. 시간은 문제를 시작할 때부터 흐르고 되돌아갈 수 없다.
 * 차시를 닫거나 마감해도 이미 시작한 문제는 제 마감 시각까지 제출을 받고, 새로 시작하는 것만 막는다.
 */
@Service
@Transactional
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
) {
    fun findAttempt(command: AttemptCommand.Query): AttemptDto {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        return toDto(lesson)
    }

    fun start(command: AttemptCommand.Start): AttemptDto {
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
        return toDto(lesson.copy(attempt = attempt))
    }

    fun saveDraft(command: AttemptCommand.Answer): AttemptDto {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        val answer = inProgressAnswer(lesson, command.questionIndex)
        assertCheck(!LocalDateTime.now(clock).isAfter(answer.deadline)) { ErrorCode.ATTEMPT_TIME_OVER }
        assertCheck(answer.question.accepts(command.optionId)) { ErrorCode.SUBMISSION_ANSWER_INVALID }
        answer.saveDraft(command.optionId, command.text?.trim())
        return toDto(lesson)
    }

    /** 이미 확정된 문제에 다시 제출하면 확정 결과를 그대로 돌려준다. 재전송이 안전해야 한다. */
    fun submit(command: AttemptCommand.Answer): AttemptDto {
        val lesson = load(command.userId, command.classroomId, command.lessonNumber)
        val attempt = lesson.attempt ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        val answer =
            attemptAnswerRepository.findByAttemptIdAndQuestionIndex(requireNotNull(attempt.id), command.questionIndex)
                ?: throw BusinessException(ErrorCode.ATTEMPT_NOT_IN_QUESTION)
        if (answer.isFinalized) return toDto(lesson)

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
        return toDto(lesson)
    }

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

    private fun toDto(lesson: LessonAttempt): AttemptDto {
        val attempt = lesson.attempt
        val answers =
            attempt?.id?.let(attemptAnswerRepository::findAllByAttemptIdOrderByQuestionIndexAsc).orEmpty()
        val phase = attempt?.phase ?: AttemptPhase.READY
        val nextIndex = attempt?.nextIndex ?: 0
        val finalized = answers.filter { it.isFinalized }
        return AttemptDto(
            phase = phase,
            total = lesson.questions.size,
            nextIndex = nextIndex,
            nextTimeLimitSec = lesson.questions.getOrNull(nextIndex)?.timeLimitSec,
            correctCount =
            if (phase == AttemptPhase.DONE) finalized.count { it.finalResult == QuestionResult.RIGHT } else null,
            current =
            answers
                .firstOrNull { !it.isFinalized && it.questionIndex == nextIndex }
                ?.takeIf { phase == AttemptPhase.IN_QUESTION }
                ?.let(::toCurrent),
            last = finalized.maxByOrNull { it.questionIndex }?.let(::toFinalized),
        )
    }

    private fun toCurrent(answer: AttemptAnswer) = CurrentQuestionDto(
        questionIndex = answer.questionIndex,
        question = questionImageUrlResolver.toDtos(listOf(answer.question)).single(),
        remainingMillis = answer.remainingMillis(LocalDateTime.now(clock)),
        draftOptionId = answer.optionId,
        draftText = answer.answerText,
    )

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

    private data class LessonAttempt(
        val classroom: Classroom,
        val opening: LessonOpening,
        val questions: List<Question>,
        val attempt: Attempt?,
    )
}
