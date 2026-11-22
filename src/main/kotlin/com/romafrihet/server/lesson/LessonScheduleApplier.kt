package com.romafrihet.server.lesson

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * 시각이 지난 예약을 적용한다. 반의 차시 상태를 읽기 전에 부른다.
 * 쓰기이므로 호출 측 트랜잭션은 readOnly가 아니어야 한다.
 *
 * - 열기: 문제가 있고, 마감되지 않았고, 반에 다른 열린 차시가 없을 때만 연다. 다른 차시가 열려 있으면 다음에 다시 본다.
 * - 마감: 열린 기록이 있으면 마감하고, 열린 적이 없으면 예약만 지운다.
 * - 기록의 시각은 지금이 아니라 예약한 시각으로 남긴다. 늦게 읽혀도 언제 열리고 마감됐는지가 맞아야 한다.
 */
@Component
class LessonScheduleApplier(
    private val lessonScheduleRepository: LessonScheduleRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val lessonRepository: LessonRepository,
    private val clock: Clock,
) {
    fun applyDue(classroomIds: Collection<Long>) {
        if (classroomIds.isEmpty()) return
        val now = LocalDateTime.now(clock)
        val due = lessonScheduleRepository.findDue(classroomIds, now).sortedBy { it.firstAt }
        if (due.isEmpty()) return
        val openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(due.map { requireNotNull(it.classroom.id) }.toSet())
                .associateBy { requireNotNull(it.classroom.id) to it.lessonNumber }
                .toMutableMap()
        val withQuestions =
            lessonRepository
                .findLessonsWithQuestions(due.map { requireNotNull(it.classroom.course.id) }.toSet())
                .map { it.courseId to it.lessonNumber }
                .toSet()
        val created = mutableListOf<LessonOpening>()
        due.forEach { schedule ->
            val opened = applyOpen(schedule, now, openings, withQuestions)?.also(created::add)
            if (schedule.openAt == null) applyFinish(schedule, now, opened ?: openings[schedule.key])
        }
        // 다 쓴 예약은 먼저 지운다. 두 시각이 모두 빈 채로 수정되면 ck_lesson_schedules_has_time에 걸린다.
        lessonScheduleRepository.deleteAll(due.filter { it.isEmpty })
        // 마감(수정)이 먼저 반영돼야 같은 반에 새 차시를 넣을 때 "반마다 열린 차시 하나" 인덱스에 걸리지 않는다.
        lessonOpeningRepository.flush()
        lessonOpeningRepository.saveAll(created)
    }

    /** 새로 만든 기록을 돌려준다. 다른 차시가 열려 있어 미룬 예약은 openAt을 그대로 둔다. */
    private fun applyOpen(
        schedule: LessonSchedule,
        now: LocalDateTime,
        openings: MutableMap<Pair<Long, Int>, LessonOpening>,
        withQuestions: Set<Pair<Long, Int>>,
    ): LessonOpening? {
        val openAt = schedule.openAt?.takeIf { !it.isAfter(now) }
        val classroomId = requireNotNull(schedule.classroom.id)
        val existing = openings[schedule.key]
        val openable =
            (requireNotNull(schedule.classroom.course.id) to schedule.lessonNumber) in withQuestions &&
                existing?.status != LessonStatus.FINISHED
        val busy = openings.values.any { it.classroom.id == classroomId && it.isOpen && it !== existing }
        return when {
            openAt == null || (openable && busy) -> null

            // 그사이 문제가 지워졌거나 직접 마감했다. 더 할 일이 없으므로 예약을 비운다.
            !openable -> {
                schedule.clearOpen()
                schedule.clearFinish()
                null
            }

            existing != null -> {
                schedule.clearOpen()
                if (existing.status == LessonStatus.CLOSED) existing.reopen(openAt)
                null
            }

            else -> {
                schedule.clearOpen()
                LessonOpening.create(schedule.classroom, schedule.lessonNumber, openAt).also {
                    openings[schedule.key] = it
                }
            }
        }
    }

    private fun applyFinish(schedule: LessonSchedule, now: LocalDateTime, opening: LessonOpening?) {
        val finishAt = schedule.finishAt?.takeIf { !it.isAfter(now) } ?: return
        if (opening != null && opening.status != LessonStatus.FINISHED) opening.finish(finishAt)
        schedule.clearFinish()
    }

    private val LessonSchedule.key: Pair<Long, Int>
        get() = requireNotNull(classroom.id) to lessonNumber
}
