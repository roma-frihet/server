package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.dto.ClassroomLessonStatusDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LessonOpeningService(
    private val classroomRepository: ClassroomRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val lessonRepository: LessonRepository,
) {
    fun findStatuses(userId: UUID): List<ClassroomLessonStatusDto> {
        val classrooms = classroomRepository.findAllByCourseUserIdOrderByNameAsc(userId)
        val openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(classrooms.map { requireNotNull(it.id) })
                .groupBy { requireNotNull(it.classroom.id) }
        val lastLessonNumbers =
            lessonRepository
                .findAllByCourseIdIn(classrooms.map { requireNotNull(it.course.id) }.toSet())
                .groupBy { requireNotNull(it.course.id) }
                .mapValues { (_, lessons) -> lessons.maxOf { it.lessonNumber } }
        return classrooms.map {
            toStatus(it, openings[it.id].orEmpty(), lastLessonNumbers[it.course.id] ?: 0)
        }
    }

    @Transactional
    fun open(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val classroomId = requireNotNull(classroom.id)
        val courseId = requireNotNull(classroom.course.id)
        if (!lessonRepository.existsByCourseIdAndLessonNumber(courseId, command.lessonNumber)) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }
        assertCheck(questionRepository.existsByCourseIdAndLessonNumber(courseId, command.lessonNumber)) {
            ErrorCode.LESSON_HAS_NO_QUESTIONS
        }
        val previous = lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)
        assertCheck(previous?.status != LessonStatus.FINISHED) { ErrorCode.LESSON_FINISHED }
        assertCheck(lessonOpeningRepository.findByClassroomIdAndClosedAtIsNull(classroomId) == null) {
            ErrorCode.LESSON_ALREADY_OPEN
        }

        if (previous != null) {
            previous.reopen()
        } else {
            lessonOpeningRepository.save(LessonOpening.create(classroom, command.lessonNumber))
        }
    }

    @Transactional
    fun close(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(requireNotNull(classroom.id), command.lessonNumber)
        assertCheck(opening != null && opening.isOpen) { ErrorCode.LESSON_NOT_OPEN }
        opening.close()
    }

    @Transactional
    fun finish(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(requireNotNull(classroom.id), command.lessonNumber)
                ?: throw BusinessException(ErrorCode.LESSON_NOT_OPEN)
        opening.finish()
    }

    /** 열린 차시가 있으면 그 차시, 없으면 열었던 차시의 다음 번호가 현재 차시다. 마지막 차시 뒤에는 null이다. */
    private fun toStatus(
        classroom: Classroom,
        openings: List<LessonOpening>,
        lastLessonNumber: Int,
    ): ClassroomLessonStatusDto {
        val open = openings.firstOrNull { it.isOpen }
        val latest = openings.maxOfOrNull { it.lessonNumber }
        val next = (latest ?: 0) + 1
        return ClassroomLessonStatusDto(
            classroomId = requireNotNull(classroom.id),
            courseId = requireNotNull(classroom.course.id),
            classroomName = classroom.name,
            currentLessonNumber = open?.lessonNumber ?: next.takeIf { it <= lastLessonNumber },
            latestLessonNumber = open?.lessonNumber ?: latest,
            isOpen = open != null,
        )
    }
}
