package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.command.AttemptCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 반에서 내보낸 학생의 응시를 지운다. 되돌리는 기능이라 흔적을 남기지 않는다. */
@Service
@Transactional(readOnly = true)
class AttemptCleaner(
    private val attemptRepository: AttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val resultOverrideRepository: ResultOverrideRepository,
) {
    @Transactional
    fun deleteAllInClassroom(command: AttemptCommand.DeleteAllInClassroom) {
        attemptRepository
            .findByClassroomIdAndUserId(command.classroomId, command.userId)
            .forEach { attempt ->
                val attemptId = requireNotNull(attempt.id)
                resultOverrideRepository.deleteAllByAttemptAnswerAttemptId(attemptId)
                attemptAnswerRepository.deleteAllByAttemptId(attemptId)
            }
        attemptRepository.deleteAllByClassroomIdAndUserId(command.classroomId, command.userId)
    }
}
