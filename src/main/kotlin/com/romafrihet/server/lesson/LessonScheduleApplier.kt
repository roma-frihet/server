package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * 시각이 지난 예약을 적용한다. 반의 차시 상태를 읽기 전에 부른다.
 * 쓰기이므로 호출 측 트랜잭션은 readOnly가 아니어야 한다.
 *
 * - 열기: 문제가 있고, 마감되지 않았고, 반에 다른 열린 차시가 없을 때만 연다. 다른 차시가 열려 있으면 다음에 다시 본다.
 * - 이미 마감 시각이 지난 열기는 열지 않고 예약을 지운다. 열자마자 마감하면 학생이 한 번도 풀 수 없고
 *   마감은 되돌릴 수 없지만, 열지 않은 차시는 교사가 언제든 직접 열 수 있다.
 * - 마감: 열린 기록이 있으면 마감하고, 열린 적이 없으면 예약만 지운다.
 * - 기록의 시각은 지금이 아니라 예약한 시각으로 남긴다. 늦게 읽혀도 언제 열리고 마감됐는지가 맞아야 한다.
 */
@Component
class LessonScheduleApplier(
    private val lessonScheduleRepository: LessonScheduleRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val lessonRepository: LessonRepository,
    private val classroomRepository: ClassroomRepository,
    private val clock: Clock,
) {
    fun applyDue(classroomIds: Collection<Long>) {
        if (classroomIds.isEmpty()) return
        val now = LocalDateTime.now(clock)
        val due = lessonScheduleRepository.findDue(classroomIds, now).sortedWith(APPLY_ORDER)
        if (due.isEmpty()) return
        val dueClassroomIds = due.map { requireNotNull(it.classroom.id) }.toSet()
        // 교사의 직접 열기와 동시에 일어나도 "반마다 열린 차시 하나"를 지키도록, 반을 잠근 뒤에 열린 기록을 읽는다.
        // 예약이 있을 때만 잠그므로 학생들이 목록을 읽을 때마다 반을 잠그지는 않는다.
        classroomRepository.findAllForUpdate(dueClassroomIds)
        val openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(dueClassroomIds)
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
        val openable = isOpenable(schedule, now, existing, withQuestions)
        val busy = openings.values.any { it.classroom.id == classroomId && it.isOpen && it !== existing }
        return when {
            openAt == null || (openable && busy) -> null

            // 마감 시각이 이미 지났거나, 그사이 문제가 지워졌거나 직접 마감했다. 열지 않고 예약을 비운다.
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

    /** 마감 시각이 이미 지났으면 열지 않는다. 문제가 없거나 이미 마감한 차시도 열 수 없다. */
    private fun isOpenable(
        schedule: LessonSchedule,
        now: LocalDateTime,
        existing: LessonOpening?,
        withQuestions: Set<Pair<Long, Int>>,
    ): Boolean {
        val expired = schedule.finishAt?.let { !it.isAfter(now) } ?: false
        val hasQuestions = (requireNotNull(schedule.classroom.course.id) to schedule.lessonNumber) in withQuestions
        return !expired && hasQuestions && existing?.status != LessonStatus.FINISHED
    }

    private fun applyFinish(schedule: LessonSchedule, now: LocalDateTime, opening: LessonOpening?) {
        val finishAt = schedule.finishAt?.takeIf { !it.isAfter(now) } ?: return
        if (opening != null && opening.status != LessonStatus.FINISHED) opening.finish(finishAt)
        schedule.clearFinish()
    }

    private val LessonSchedule.key: Pair<Long, Int>
        get() = requireNotNull(classroom.id) to lessonNumber

    private companion object {
        /** 먼저 일어날 일부터 적용하고, 같은 시각이면 마감을 먼저 해야 다음 차시를 열 수 있다. */
        val APPLY_ORDER: Comparator<LessonSchedule> =
            compareBy<LessonSchedule> { it.firstAt }.thenBy { if (it.openAt == null) 0 else 1 }
    }
}
