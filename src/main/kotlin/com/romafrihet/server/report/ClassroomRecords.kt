package com.romafrihet.server.report

import com.romafrihet.server.classroom.ClassroomMembership
import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.lesson.AttemptFinalizer
import com.romafrihet.server.lesson.AttemptRepository
import com.romafrihet.server.lesson.LessonOpening
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.projection.AttemptScoreProjection
import org.springframework.stereotype.Component
import java.util.UUID

/** 집계에 필요한 반의 기록이다. 반 단위로 조회하므로 메모리에서 묶어도 크기가 작다. */
data class ClassroomRecords(
    val students: Map<Long, List<ClassroomMembership>>,
    val openings: Map<Long, List<LessonOpening>>,
    val scores: Map<Long, Map<UUID, Map<Int, AttemptScoreProjection>>>,
    val questionCounts: Map<Int, Int>,
)

@Component
class ClassroomRecordsReader(
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val attemptRepository: AttemptRepository,
    private val attemptFinalizer: AttemptFinalizer,
    private val questionRepository: QuestionRepository,
) {
    /** 격자까지 읽을 필요 없이 확정만 할 때 쓴다. 쓰기이므로 호출 측 트랜잭션은 readOnly가 아니어야 한다. */
    fun finalizeExpired(courseId: Long) {
        attemptFinalizer.finalizeExpired(
            attemptRepository.findAllByClassroomCourseId(courseId),
            questionCounts(courseId),
        )
    }

    fun read(courseId: Long, classroomIds: List<Long>): ClassroomRecords {
        val questionCounts = questionCounts(courseId)
        // 학생이 돌아오지 않아도 점수가 맞도록, 세기 전에 시간이 지난 응답부터 확정한다.
        attemptFinalizer.finalizeExpired(attemptRepository.findAllByClassroomIdIn(classroomIds), questionCounts)
        return ClassroomRecords(
            students =
            classroomMembershipRepository
                .findAllByClassroomIdIn(classroomIds)
                .groupBy { requireNotNull(it.classroom.id) },
            openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(classroomIds)
                .groupBy { requireNotNull(it.classroom.id) },
            scores =
            attemptRepository
                .findScores(classroomIds)
                .groupBy { it.classroomId }
                .mapValues { (_, rows) ->
                    rows.groupBy { it.userId }.mapValues { (_, byUser) -> byUser.associateBy { it.lessonNumber } }
                },
            questionCounts = questionCounts,
        )
    }

    private fun questionCounts(courseId: Long): Map<Int, Int> = questionRepository
        .findQuestionCounts(courseId)
        .associate { it.lessonNumber to it.questionCount.toInt() }
}
