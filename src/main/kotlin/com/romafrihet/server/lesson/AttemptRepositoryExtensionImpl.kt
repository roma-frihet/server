package com.romafrihet.server.lesson

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.romafrihet.server.classroom.QClassroom
import com.romafrihet.server.classroom.QClassroomMembership
import com.romafrihet.server.lesson.dto.LessonScoreQueryDto
import com.romafrihet.server.lesson.projection.LessonScoreProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

/**
 * 연결된 학생과 그 반에서 열었던 차시를 곱해, 풀지 않은 학생도 행으로 남긴다.
 * 점수는 저장하지 않으므로 맞힌 개수와 응답 개수를 그때그때 센다.
 */
class AttemptRepositoryExtensionImpl(private val queryFactory: JPAQueryFactory) : AttemptRepositoryExtension {
    private val membership = QClassroomMembership.classroomMembership
    private val classroom = QClassroom.classroom
    private val opening = QLessonOpening.lessonOpening
    private val attempt = QAttempt.attempt
    private val answer = QAttemptAnswer.attemptAnswer
    private val question = QQuestion.question

    override fun queryScores(query: LessonScoreQueryDto, pageable: Pageable): Page<LessonScoreProjection> {
        val conditions = conditionsOf(query)
        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        LessonScoreProjection::class.java,
                        classroom.id,
                        classroom.classNo,
                        membership.id,
                        membership.studentName,
                        membership.studentNumber,
                        opening.lessonNumber,
                        countOf(QuestionResult.RIGHT, equal = true),
                        countOf(QuestionResult.NONE, equal = false),
                        questionCount(),
                    ),
                ).from(membership)
                .join(membership.classroom, classroom)
                .join(opening)
                .on(opening.classroom.id.eq(classroom.id))
                .where(*conditions)
                .orderBy(
                    classroom.classNo.asc(),
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

    /** 바깥의 반, 학생, 차시에 걸린 상관 서브쿼리다. */
    private fun countOf(result: QuestionResult, equal: Boolean) = JPAExpressions
        .select(answer.count())
        .from(answer)
        .join(answer.attempt, attempt)
        .where(
            attempt.classroom.id.eq(classroom.id),
            attempt.userId.eq(membership.userId),
            attempt.lessonNumber.eq(opening.lessonNumber),
            if (equal) answer.finalResult.eq(result) else answer.finalResult.ne(result),
        )

    private fun questionCount() = JPAExpressions
        .select(question.count())
        .from(question)
        .where(
            question.course.id.eq(classroom.course.id),
            question.lessonNumber.eq(opening.lessonNumber),
        )

    private fun conditionsOf(query: LessonScoreQueryDto): Array<Predicate> = listOfNotNull(
        classroom.course.id.eq(query.courseId),
        query.classroomId?.let(classroom.id::eq),
        query.lessonNumber?.let(opening.lessonNumber::eq),
    ).toTypedArray()
}
