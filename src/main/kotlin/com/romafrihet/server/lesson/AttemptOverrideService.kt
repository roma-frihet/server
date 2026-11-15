package com.romafrihet.server.lesson

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
) {
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
