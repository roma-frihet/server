package com.romafrihet.server.lesson

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.classroom.QClassroom
import com.romafrihet.server.classroom.QClassroomMembership
import com.romafrihet.server.lesson.dto.LessonScoreQueryDto
import com.romafrihet.server.lesson.projection.LessonScoreProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

/** 승인된 학생과 그 반에서 열었던 차시를 곱해, 제출하지 않은 학생도 행으로 남긴다. */
class LessonSubmissionRepositoryExtensionImpl(private val queryFactory: JPAQueryFactory) :
    LessonSubmissionRepositoryExtension {
    private val membership = QClassroomMembership.classroomMembership
    private val classroom = QClassroom.classroom
    private val opening = QLessonOpening.lessonOpening
    private val submission = QLessonSubmission.lessonSubmission

    override fun queryScores(query: LessonScoreQueryDto, pageable: Pageable): Page<LessonScoreProjection> {
        val conditions = conditionsOf(query)
        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        LessonScoreProjection::class.java,
                        classroom.id,
                        classroom.name,
                        membership.id,
                        membership.studentName,
                        membership.studentNumber,
                        opening.lessonNumber,
                        submission.correctCount,
                        submission.questionCount,
                    ),
                ).from(membership)
                .join(membership.classroom, classroom)
                .join(opening)
                .on(opening.classroom.id.eq(classroom.id))
                .leftJoin(submission)
                .on(
                    submission.classroom.id.eq(classroom.id),
                    submission.userId.eq(membership.userId),
                    submission.lessonNumber.eq(opening.lessonNumber),
                ).where(*conditions)
                .orderBy(
                    classroom.name.asc(),
                    classroom.id.asc(),
                    membership.studentNumber.asc(),
                    membership.id.asc(),
                    opening.lessonNumber.asc(),
                ).offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()
        val total =
            queryFactory
                .select(membership.count())
                .from(membership)
                .join(membership.classroom, classroom)
                .join(opening)
                .on(opening.classroom.id.eq(classroom.id))
                .where(*conditions)
                .fetchOne() ?: 0L
        return PageImpl(content, pageable, total)
    }

    private fun conditionsOf(query: LessonScoreQueryDto): Array<Predicate> = listOfNotNull(
        classroom.course.id.eq(query.courseId),
        membership.status.eq(MembershipStatus.APPROVED),
        query.classroomId?.let(classroom.id::eq),
        query.lessonNumber?.let(opening.lessonNumber::eq),
    ).toTypedArray()
}
