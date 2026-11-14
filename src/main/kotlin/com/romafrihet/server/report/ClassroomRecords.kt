package com.romafrihet.server.report

import com.romafrihet.server.classroom.ClassroomMembership
import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.lesson.LessonOpening
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.LessonSubmission
import com.romafrihet.server.lesson.LessonSubmissionRepository
import com.romafrihet.server.lesson.QuestionRepository
import org.springframework.stereotype.Component

/** 집계에 필요한 반의 기록이다. 반 단위로 조회하므로 메모리에서 묶어도 크기가 작다. */
data class ClassroomRecords(
    val students: Map<Long, List<ClassroomMembership>>,
    val openings: Map<Long, List<LessonOpening>>,
    val submissions: Map<Long, List<LessonSubmission>>,
    val questionCounts: Map<Int, Int>,
)

@Component
class ClassroomRecordsReader(
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val lessonSubmissionRepository: LessonSubmissionRepository,
    private val questionRepository: QuestionRepository,
) {
    fun read(courseId: Long, classroomIds: List<Long>): ClassroomRecords = ClassroomRecords(
        students =
        classroomMembershipRepository
            .findAllByClassroomIdInAndStatus(classroomIds, MembershipStatus.APPROVED)
            .groupBy { requireNotNull(it.classroom.id) },
        openings =
        lessonOpeningRepository
            .findAllByClassroomIdIn(classroomIds)
            .groupBy { requireNotNull(it.classroom.id) },
        submissions =
        lessonSubmissionRepository
            .findAllByClassroomIdIn(classroomIds)
            .groupBy { requireNotNull(it.classroom.id) },
        questionCounts =
        questionRepository
            .findQuestionCounts(courseId)
            .associate { it.lessonNumber to it.questionCount.toInt() },
    )
}
