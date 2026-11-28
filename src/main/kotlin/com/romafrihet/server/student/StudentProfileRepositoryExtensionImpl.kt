package com.romafrihet.server.student

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.romafrihet.server.school.QSchool
import com.romafrihet.server.student.dto.StudentProfileQueryDto
import com.romafrihet.server.student.projection.StudentProfileSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class StudentProfileRepositoryExtensionImpl(private val queryFactory: JPAQueryFactory) :
    StudentProfileRepositoryExtension {
    private val profile = QStudentProfile.studentProfile
    private val school = QSchool.school

    /** 학교, 학번 순이다. */
    override fun queryProfiles(
        query: StudentProfileQueryDto,
        pageable: Pageable,
    ): Page<StudentProfileSummaryProjection> {
        val conditions = conditionsOf(query)
        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        StudentProfileSummaryProjection::class.java,
                        profile.userId,
                        school.id,
                        school.name,
                        profile.grade,
                        profile.classNumber,
                        profile.seatNumber,
                        profile.name,
                        profile.mustChangePassword,
                        profile.createdAt,
                    ),
                ).from(profile)
                .join(profile.school, school)
                .where(*conditions)
                .orderBy(
                    school.name.asc(),
                    profile.grade.asc(),
                    profile.classNumber.asc(),
                    profile.seatNumber.asc(),
                    profile.id.asc(),
                ).offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()
        val total = queryFactory.select(profile.count()).from(profile).where(*conditions).fetchOne() ?: 0L
        return PageImpl(content, pageable, total)
    }

    private fun conditionsOf(query: StudentProfileQueryDto): Array<Predicate> = listOfNotNull(
        query.schoolId?.let(profile.school.id::eq),
        query.grade?.let(profile.grade::eq),
        query.classNumber?.let(profile.classNumber::eq),
        query.keyword?.takeIf { it.isNotBlank() }?.let { profile.name.contains(it.trim()) },
    ).toTypedArray()
}
