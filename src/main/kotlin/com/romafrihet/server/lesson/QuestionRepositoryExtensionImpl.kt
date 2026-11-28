package com.romafrihet.server.lesson

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.romafrihet.server.course.QCourse
import com.romafrihet.server.lesson.dto.QuestionAdminQueryDto
import com.romafrihet.server.lesson.projection.QuestionSummaryProjection
import com.romafrihet.server.school.QSchool
import com.romafrihet.server.subject.QSubject
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class QuestionRepositoryExtensionImpl(private val queryFactory: JPAQueryFactory) : QuestionRepositoryExtension {
    private val question = QQuestion.question
    private val course = QCourse.course
    private val school = QSchool.school
    private val subject = QSubject.subject
    private val lesson = QLesson.lesson

    /** 최근에 만든 문제부터 본다. */
    override fun queryForAdmin(query: QuestionAdminQueryDto, pageable: Pageable): Page<QuestionSummaryProjection> {
        val conditions = conditionsOf(query)
        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        QuestionSummaryProjection::class.java,
                        question.id,
                        course.id,
                        course.userId,
                        school.name,
                        course.grade,
                        subject.name,
                        question.lessonNumber,
                        lesson.title,
                        question.type,
                        question.content,
                        question.imageId,
                        question.createdAt,
                    ),
                ).from(question)
                .join(question.course, course)
                .join(course.school, school)
                .join(course.subject, subject)
                .join(lesson)
                .on(lesson.course.id.eq(course.id), lesson.lessonNumber.eq(question.lessonNumber))
                .where(*conditions)
                .orderBy(question.createdAt.desc(), question.id.desc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()
        val total =
            queryFactory
                .select(question.count())
                .from(question)
                .join(question.course, course)
                .where(*conditions)
                .fetchOne() ?: 0L
        return PageImpl(content, pageable, total)
    }

    private fun conditionsOf(query: QuestionAdminQueryDto): Array<Predicate> = listOfNotNull(
        query.schoolId?.let(course.school.id::eq),
        query.subjectId?.let(course.subject.id::eq),
        query.teacherUserId?.let(course.userId::eq),
        query.type?.let(question.type::eq),
        query.keyword?.takeIf { it.isNotBlank() }?.let { question.content.containsIgnoreCase(it.trim()) },
    ).toTypedArray()
}
