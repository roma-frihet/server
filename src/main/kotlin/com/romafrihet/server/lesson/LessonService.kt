package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.LessonClassroomStatusDto
import com.romafrihet.server.lesson.dto.LessonDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class LessonService(
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonRepository: LessonRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val questionCleaner: QuestionCleaner,
    private val attemptRepository: AttemptRepository,
    private val lessonScheduleRepository: LessonScheduleRepository,
    private val lessonScheduleApplier: LessonScheduleApplier,
    private val clock: Clock,
) {
    @Transactional
    fun create(command: LessonCommand.Create): LessonDto {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        val lessonNumber = (lessonRepository.findMaxLessonNumber(command.courseId) ?: 0) + 1
        lessonRepository.save(Lesson.create(course, lessonNumber, command))
        return find(command.courseId).single { it.lessonNumber == lessonNumber }
    }

    /**
     * 차시를 지우면 그 차시의 문제와 연 기록도 함께 사라진다. 학생이 푼 적이 있으면 막는다.
     * 번호는 다시 쓰지 않는다. 2차시를 지워도 3차시가 2차시가 되지는 않는다.
     */
    @Transactional
    fun delete(command: LessonCommand.Delete) {
        courseRepository.getOwned(command.courseId, command.userId)
        val lesson = lessonRepository.getByNumber(command.courseId, command.lessonNumber)
        assertCheck(
            !attemptRepository.existsByClassroomCourseIdAndLessonNumber(command.courseId, command.lessonNumber),
        ) { ErrorCode.LESSON_HAS_ATTEMPTS }

        lessonScheduleRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        lessonOpeningRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        questionCleaner.deleteAllInLesson(
            QuestionCommand.DeleteAllInLesson(command.courseId, command.lessonNumber),
        )
        lessonRepository.delete(lesson)
    }

    @Transactional
    fun rename(command: LessonCommand.Rename) {
        courseRepository.getOwned(command.courseId, command.userId)
        lessonRepository.getByNumber(command.courseId, command.lessonNumber).rename(command)
    }

    /** 읽기 전에 시각이 지난 예약을 적용하므로 readOnly가 아니다. */
    @Transactional
    fun findAll(command: LessonCommand.Query): List<LessonDto> {
        courseRepository.getOwned(command.courseId, command.userId)
        return find(command.courseId)
    }

    private fun find(courseId: Long): List<LessonDto> {
        val classrooms = classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId)
        val classroomIds = classrooms.map { requireNotNull(it.id) }
        lessonScheduleApplier.applyDue(classroomIds)
        val openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(classroomIds)
                .associateBy { requireNotNull(it.classroom.id) to it.lessonNumber }
        val schedules =
            lessonScheduleRepository
                .findAllByClassroomIdIn(classroomIds)
                .associateBy { requireNotNull(it.classroom.id) to it.lessonNumber }
        val zone = clock.zone
        val counts = questionRepository.findQuestionCounts(courseId).associateBy { it.lessonNumber }
        return lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId).map { lesson ->
            LessonDto(
                lessonNumber = lesson.lessonNumber,
                title = lesson.title,
                questionCount = counts[lesson.lessonNumber]?.questionCount ?: 0,
                classrooms =
                classrooms.map { classroom ->
                    val classroomId = requireNotNull(classroom.id)
                    val schedule = schedules[classroomId to lesson.lessonNumber]
                    LessonClassroomStatusDto(
                        classroomId = classroomId,
                        classNo = classroom.classNo,
                        status = openings[classroomId to lesson.lessonNumber]?.status ?: LessonStatus.PLANNED,
                        scheduledOpenAt = schedule?.openAt?.atZone(zone)?.toOffsetDateTime(),
                        scheduledFinishAt = schedule?.finishAt?.atZone(zone)?.toOffsetDateTime(),
                    )
                },
            )
        }
    }
}
