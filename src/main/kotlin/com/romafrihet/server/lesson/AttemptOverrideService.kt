package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.AttemptOverrideCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 접속이 끊기는 등 사정이 있으면 교사가 학생 이야기를 듣고 결과를 바꾼다.
 * 원래 결과는 지우지 않고 ResultOverride에 남긴다.
 */
@Service
@Transactional(readOnly = true)
class AttemptOverrideService(
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val resultOverrideRepository: ResultOverrideRepository,
    private val classroomRepository: ClassroomRepository,
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
    private val attemptFinalizer: AttemptFinalizer,
) {
    /**
     * 주관식에서 같은 답을 쓴 학생을 한 번에 인정하거나 취소한다. 바뀐 응답 수를 돌려준다.
     * 이미 그 결과인 응답은 이력을 남기지 않고 건너뛴다.
     */
    @Transactional
    fun overrideByAnswer(command: AttemptOverrideCommand.ByAnswer): Int {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val question = questionRepository.getOwned(command.questionId, command.userId)
        assertCheck(question.course.id == classroom.course.id && question.lessonNumber == command.lessonNumber) {
            ErrorCode.NOT_FOUND
        }
        assertCheck(question.type == QuestionType.SHORT_ANSWER) { ErrorCode.QUESTION_NOT_SHORT_ANSWER }
        assertCheck(command.result != QuestionResult.NONE) { ErrorCode.INVALID_VALUE }
        // 시간이 지났는데 아직 확정되지 않은 응답부터 확정한다. 건너뛰면 같은 답을 쓴 학생이 인정에서 빠지고,
        // 나중에 다른 화면에서 확정될 때 오답으로 남는다.
        attemptFinalizer.finalizeExpired(
            attemptRepository.findAllByClassroomIdInAndLessonNumber(listOf(command.classroomId), command.lessonNumber),
            mapOf(
                command.lessonNumber to
                    questionRepository.countByCourseIdAndLessonNumber(
                        requireNotNull(question.course.id),
                        command.lessonNumber,
                    ),
            ),
        )
        val target = normalizeAnswer(command.answerText)
        val answers =
            attemptAnswerRepository
                .findFinalizedTextAnswers(command.classroomId, command.lessonNumber, command.questionId)
                .filter { normalizeAnswer(requireNotNull(it.answerText)) == target && it.finalResult != command.result }
        resultOverrideRepository.saveAll(answers.map { ResultOverride.create(it, command.userId, command.result) })
        answers.forEach { it.overrideResult(command.result) }
        return answers.size
    }

    @Transactional
    fun override(command: AttemptOverrideCommand.Update) {
        val answer =
            attemptAnswerRepository
                .findByIdAndAttemptClassroomCourseUserId(command.attemptAnswerId, command.userId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        assertCheck(answer.isFinalized) { ErrorCode.ANSWER_NOT_FINALIZED }
        resultOverrideRepository.save(ResultOverride.create(answer, command.userId, command.result))
        answer.overrideResult(command.result)
    }
}
