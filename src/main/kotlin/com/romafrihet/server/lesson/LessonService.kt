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
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val resultOverrideRepository: ResultOverrideRepository,
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
     * 차시를 지우면 그 차시의 문제와 푼 기록까지 함께 사라진다. 반이나 수업을 지울 때와 같은 기준이다.
     * 지금 열려 있는 반이 하나라도 있으면 막는다. 풀고 있는 학생의 문제가 사라지면 남은 문제를
     * 건너뛰게 되기 때문이다. 번호는 다시 쓰지 않는다. 2차시를 지워도 3차시가 2차시가 되지는 않는다.
     */
    @Transactional
    fun delete(command: LessonCommand.Delete) {
        courseRepository.getOwned(command.courseId, command.userId)
        val lesson = lessonRepository.getByNumber(command.courseId, command.lessonNumber)
        validateNotOpen(command.courseId, command.lessonNumber)

        // 외래 키 순서대로 치운다. 재채점 → 응답 → 응시 → 연 기록 → 예약 → 문제·보기 → 차시다.
        resultOverrideRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        attemptAnswerRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        attemptRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        lessonOpeningRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        lessonScheduleRepository.deleteAllByCourseIdAndLessonNumber(command.courseId, command.lessonNumber)
        questionCleaner.deleteAllInLesson(
            QuestionCommand.DeleteAllInLesson(command.courseId, command.lessonNumber),
        )
        lessonRepository.delete(lesson)
    }

    /** 열기 예약이 적용되지 않은 채로 판단하지 않도록 먼저 적용한다. */
    private fun validateNotOpen(courseId: Long, lessonNumber: Int) {
        lessonScheduleApplier.applyDue(
            classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId).map { requireNotNull(it.id) },
        )
        assertCheck(
            !lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId, lessonNumber),
        ) { ErrorCode.LESSON_OPEN_NOW }
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
        val attemptedCounts = attemptRepository.queryAttemptedStudentCounts(courseId).associateBy { it.lessonNumber }
        return lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId).map { lesson ->
            LessonDto(
                lessonNumber = lesson.lessonNumber,
                title = lesson.title,
                questionCount = counts[lesson.lessonNumber]?.questionCount ?: 0,
                attemptedStudentCount = attemptedCounts[lesson.lessonNumber]?.attemptedStudentCount ?: 0,
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
