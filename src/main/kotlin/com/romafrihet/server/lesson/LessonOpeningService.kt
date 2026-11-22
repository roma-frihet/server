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
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LessonOpeningService(
    private val classroomRepository: ClassroomRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val lessonRepository: LessonRepository,
    private val lessonScheduleRepository: LessonScheduleRepository,
    private val lessonScheduleApplier: LessonScheduleApplier,
    private val clock: Clock,
) {
    /** 읽기 전에 시각이 지난 예약을 적용하므로 readOnly가 아니다. */
    @Transactional
    fun findStatuses(userId: UUID): List<ClassroomLessonStatusDto> {
        val classrooms = classroomRepository.findAllByCourseUserIdOrderByCourseIdAscClassNoAsc(userId)
        lessonScheduleApplier.applyDue(classrooms.map { requireNotNull(it.id) })
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
        lessonScheduleApplier.applyDue(listOf(classroomId))
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
            previous.reopen(LocalDateTime.now(clock))
        } else {
            lessonOpeningRepository.save(
                LessonOpening.create(classroom, command.lessonNumber, LocalDateTime.now(clock)),
            )
        }
        // 직접 열었으면 열기 예약은 필요 없다. 자동 마감은 그대로 둔다.
        lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)?.let {
            it.clearOpen()
            if (it.isEmpty) lessonScheduleRepository.delete(it)
        }
    }

    @Transactional
    fun close(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        lessonScheduleApplier.applyDue(listOf(requireNotNull(classroom.id)))
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(requireNotNull(classroom.id), command.lessonNumber)
        assertCheck(opening != null && opening.isOpen) { ErrorCode.LESSON_NOT_OPEN }
        opening.close(LocalDateTime.now(clock))
    }

    @Transactional
    fun finish(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val classroomId = requireNotNull(classroom.id)
        lessonScheduleApplier.applyDue(listOf(classroomId))
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)
                ?: throw BusinessException(ErrorCode.LESSON_NOT_OPEN)
        opening.finish(LocalDateTime.now(clock))
        lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)
            ?.let(lessonScheduleRepository::delete)
    }

    /**
     * 열기 예약과 자동 마감을 건다. 같은 차시에 이미 예약이 있으면 바꾼다.
     * 열린 차시는 자동 마감만 걸 수 있고, 열리지 않은 차시는 열 시각이 있어야 한다.
     */
    @Transactional
    fun schedule(command: LessonOpeningCommand.Schedule) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val classroomId = requireNotNull(classroom.id)
        val courseId = requireNotNull(classroom.course.id)
        lessonScheduleApplier.applyDue(listOf(classroomId))
        if (!lessonRepository.existsByCourseIdAndLessonNumber(courseId, command.lessonNumber)) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }
        assertCheck(questionRepository.existsByCourseIdAndLessonNumber(courseId, command.lessonNumber)) {
            ErrorCode.LESSON_HAS_NO_QUESTIONS
        }
        val status =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)?.status
        assertCheck(status != LessonStatus.FINISHED) { ErrorCode.LESSON_FINISHED }
        val now = LocalDateTime.now(clock)
        val openAt = command.openAt?.atZoneSameInstant(clock.zone)?.toLocalDateTime()
        val finishAt = command.finishAt?.atZoneSameInstant(clock.zone)?.toLocalDateTime()
        val valid =
            (if (status == LessonStatus.OPEN) openAt == null && finishAt != null else openAt != null) &&
                (openAt == null || openAt.isAfter(now)) &&
                (finishAt == null || finishAt.isAfter(now)) &&
                (openAt == null || finishAt == null || finishAt.isAfter(openAt))
        assertCheck(valid) { ErrorCode.LESSON_SCHEDULE_INVALID }

        val existing = lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, command.lessonNumber)
        if (existing != null) {
            existing.reschedule(openAt, finishAt)
        } else {
            lessonScheduleRepository.save(LessonSchedule.create(classroom, command.lessonNumber, openAt, finishAt))
        }
    }

    @Transactional
    fun cancelSchedule(command: LessonOpeningCommand.Change) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        lessonScheduleRepository.findByClassroomIdAndLessonNumber(requireNotNull(classroom.id), command.lessonNumber)
            ?.let(lessonScheduleRepository::delete)
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
            classNo = classroom.classNo,
            currentLessonNumber = open?.lessonNumber ?: next.takeIf { it <= lastLessonNumber },
            latestLessonNumber = open?.lessonNumber ?: latest,
            isOpen = open != null,
        )
    }
}
