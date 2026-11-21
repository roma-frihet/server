package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.command.AttemptCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 응시와 그에 딸린 기록을 지운다. 되돌리는 기능이라 흔적을 남기지 않는다.
 *
 * 지우는 순서는 재채점 → 응답 → 응시다. 거꾸로 하면 외래 키에 걸린다.
 */
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

    @Transactional
    fun deleteAllInClassrooms(command: AttemptCommand.DeleteAllInClassrooms) {
        if (command.classroomIds.isEmpty()) return
        resultOverrideRepository.deleteAllByClassroomIdIn(command.classroomIds)
        attemptAnswerRepository.deleteAllByClassroomIdIn(command.classroomIds)
        attemptRepository.deleteAllByClassroomIdIn(command.classroomIds)
    }

    @Transactional
    fun deleteAllInLesson(command: AttemptCommand.DeleteAllInLesson) {
        resultOverrideRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        attemptAnswerRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        attemptRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
    }
}
