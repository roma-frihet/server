package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.report.QuestionStatsService
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class LessonScheduleFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var lessonStudentService: LessonStudentService

    @Autowired
    private lateinit var lessonScheduleRepository: LessonScheduleRepository

    @Autowired
    private lateinit var lessonOpeningRepository: LessonOpeningRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var questionStatsService: QuestionStatsService

    @Test
    fun `열 시각이 지나면 차시 목록을 읽을 때 열리고 마감 시각이 지나면 마감된다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = inHours(2))
        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.PLANNED)
        assertThat(statusOf(classroomId, 1).scheduledOpenAt).isNotNull()

        moveToPast(classroomId, "open_at")

        val opened = statusOf(classroomId, 1)
        assertThat(opened.status).isEqualTo(LessonStatus.OPEN)
        assertThat(opened.scheduledOpenAt).isNull()
        assertThat(opened.scheduledFinishAt).isNotNull()

        moveToPast(classroomId, "finish_at")

        val finished = statusOf(classroomId, 1)
        assertThat(finished.status).isEqualTo(LessonStatus.FINISHED)
        assertThat(finished.scheduledFinishAt).isNull()
        assertThat(lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, 1)).isNull()
    }

    @Test
    fun `문제별 결과를 처음 읽을 때도 예약이 반영된다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = null)
        moveToPast(classroomId, "open_at")

        val stats =
            questionStatsService.findQuestionStats(
                ReportCommand.QuestionStats(teacherId, courseId, 1, listOf(classroomId)),
            )

        assertThat(stats.single().status).isEqualTo(LessonStatus.OPEN)
    }

    @Test
    fun `학생이 차시 목록을 읽을 때도 예약이 반영된다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = null)
        moveToPast(classroomId, "open_at")

        val lessons = lessonStudentService.findOpenedLessons(studentId, classroomId)

        assertThat(lessons.map { it.lessonNumber to it.status }).containsExactly(1 to LessonStatus.OPEN)
    }

    @Test
    fun `열기 전에 마감 시각까지 지났으면 열지 않고 예약을 지운다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = inHours(2))
        jdbcTemplate.update(
            "update lesson_schedules set open_at = ?, finish_at = ? where classroom_id = ?",
            LocalDateTime.now().minusMinutes(30),
            LocalDateTime.now().minusMinutes(10),
            classroomId,
        )

        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.PLANNED)
        assertThat(lessonOpeningRepository.findByClassroomIdAndLessonNumber(classroomId, 1)).isNull()
        assertThat(lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, 1)).isNull()
        // 열지 않은 차시라 교사가 직접 열 수 있다.
        openLesson(classroomId, 1)
        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.OPEN)
    }

    @Test
    fun `다른 차시 때문에 미룬 사이 마감 시각이 지나면 열자마자 마감하지 않는다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openLesson(classroomId, 1)
        schedule(classroomId, 2, openAt = inHours(1), finishAt = inHours(2))
        moveToPast(classroomId, "open_at")
        assertThat(statusOf(classroomId, 2).status).isEqualTo(LessonStatus.PLANNED)
        jdbcTemplate.update(
            "update lesson_schedules set finish_at = ? where classroom_id = ?",
            LocalDateTime.now().minusSeconds(30),
            classroomId,
        )

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertThat(statusOf(classroomId, 2).status).isEqualTo(LessonStatus.PLANNED)
        assertThat(lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, 2)).isNull()
    }

    @Test
    fun `같은 시각의 마감과 다음 차시 열기는 마감을 먼저 적용해 한 번에 반영된다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openLesson(classroomId, 1)
        schedule(classroomId, 1, openAt = null, finishAt = inHours(1))
        schedule(classroomId, 2, openAt = inHours(1), finishAt = null)
        val same = LocalDateTime.now().minusMinutes(1)
        jdbcTemplate.update(
            "update lesson_schedules set finish_at = ? where classroom_id = ? and lesson_number = 1",
            same,
            classroomId,
        )
        jdbcTemplate.update(
            "update lesson_schedules set open_at = ? where classroom_id = ? and lesson_number = 2",
            same,
            classroomId,
        )

        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.FINISHED)
        assertThat(statusOf(classroomId, 2).status).isEqualTo(LessonStatus.OPEN)
    }

    @Test
    fun `열 시각이 지난 뒤 문제를 고치려 하면 예약부터 적용해 열린 차시로 막는다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = null)
        moveToPast(classroomId, "open_at")

        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) { createQuestion(lessonNumber = 1) }
        // 거절과 함께 적용도 롤백되지만, 예약이 남아 있어 다음 조회에서 다시 적용된다.
        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.OPEN)
    }

    @Test
    fun `다른 차시가 열려 있으면 예약 열기를 미루고 예약을 남긴다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openLesson(classroomId, 1)
        schedule(classroomId, 2, openAt = inHours(1), finishAt = null)
        moveToPast(classroomId, "open_at")

        assertThat(statusOf(classroomId, 2).status).isEqualTo(LessonStatus.PLANNED)
        assertThat(lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, 2)).isNotNull()

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertThat(statusOf(classroomId, 2).status).isEqualTo(LessonStatus.OPEN)
    }

    @Test
    fun `열린 차시에는 자동 마감만 걸 수 있고 직접 마감하면 예약이 지워진다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        assertErrorCode(ErrorCode.LESSON_SCHEDULE_INVALID) {
            schedule(classroomId, 1, openAt = inHours(1), finishAt = inHours(2))
        }
        schedule(classroomId, 1, openAt = null, finishAt = inHours(1))
        assertThat(statusOf(classroomId, 1).scheduledFinishAt).isNotNull()

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertThat(lessonScheduleRepository.findByClassroomIdAndLessonNumber(classroomId, 1)).isNull()
    }

    @Test
    fun `지난 시각이나 열기보다 이른 마감이나 문제 없는 차시는 예약할 수 없다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)

        assertErrorCode(ErrorCode.LESSON_SCHEDULE_INVALID) {
            schedule(classroomId, 1, openAt = OffsetDateTime.now().minusMinutes(1), finishAt = null)
        }
        assertErrorCode(ErrorCode.LESSON_SCHEDULE_INVALID) {
            schedule(classroomId, 1, openAt = inHours(2), finishAt = inHours(1))
        }
        assertErrorCode(ErrorCode.LESSON_SCHEDULE_INVALID) {
            schedule(classroomId, 1, openAt = null, finishAt = inHours(1))
        }
        assertErrorCode(ErrorCode.LESSON_HAS_NO_QUESTIONS) {
            schedule(classroomId, 2, openAt = inHours(1), finishAt = null)
        }
    }

    @Test
    fun `예약을 취소하면 시각이 지나도 열리지 않는다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        schedule(classroomId, 1, openAt = inHours(1), finishAt = null)

        openingService.cancelSchedule(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertThat(statusOf(classroomId, 1).scheduledOpenAt).isNull()
        assertThat(statusOf(classroomId, 1).status).isEqualTo(LessonStatus.PLANNED)
    }

    private fun inHours(hours: Long): OffsetDateTime = OffsetDateTime.now().plusHours(hours)

    private fun schedule(classroomId: Long, lessonNumber: Int, openAt: OffsetDateTime?, finishAt: OffsetDateTime?) =
        openingService.schedule(
            LessonOpeningCommand.Schedule(teacherId, classroomId, lessonNumber, openAt, finishAt),
        )

    private fun moveToPast(classroomId: Long, column: String) {
        jdbcTemplate.update(
            "update lesson_schedules set $column = ? where classroom_id = ? and $column is not null",
            LocalDateTime.now().minusMinutes(1),
            classroomId,
        )
    }

    private fun statusOf(classroomId: Long, lessonNumber: Int) = lessonService
        .findAll(LessonCommand.Query(teacherId, courseId))
        .single { it.lessonNumber == lessonNumber }
        .classrooms
        .single { it.classroomId == classroomId }
}
