package com.romafrihet.server.teacher

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.romafrihet.server.school.QSchool
import com.romafrihet.server.teacher.dto.TeacherProfileQueryDto
import com.romafrihet.server.teacher.projection.TeacherProfileSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class TeacherProfileRepositoryExtensionImpl(private val queryFactory: JPAQueryFactory) :
    TeacherProfileRepositoryExtension {
    private val profile = QTeacherProfile.teacherProfile
    private val school = QSchool.school
    private val application = QTeacherApplication.teacherApplication

    override fun queryProfiles(
        query: TeacherProfileQueryDto,
        pageable: Pageable,
    ): Page<TeacherProfileSummaryProjection> {
        val conditions = conditionsOf(query)
        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        TeacherProfileSummaryProjection::class.java,
                        profile.userId,
                        profile.name,
                        school.id,
                        school.name,
                        profile.createdAt,
                    ),
                ).from(profile)
                .join(profile.school, school)
                .where(*conditions)
                .orderBy(school.name.asc(), profile.name.asc(), profile.id.asc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()
        val total = queryFactory.select(profile.count()).from(profile).where(*conditions).fetchOne() ?: 0L
        return PageImpl(content, pageable, total)
    }

    private fun conditionsOf(query: TeacherProfileQueryDto): Array<Predicate> = listOfNotNull(
        query.schoolId?.let(profile.school.id::eq),
        query.keyword?.takeIf { it.isNotBlank() }?.trim()?.let(::nameOrEmailContains),
    ).toTypedArray()

    /** 이메일은 신청 기록에만 있다. 신청 없이 등록된 예전 교사는 이름으로만 찾는다. */
    private fun nameOrEmailContains(keyword: String): Predicate = profile.name.containsIgnoreCase(keyword).or(
        JPAExpressions
            .selectOne()
            .from(application)
            .where(application.userId.eq(profile.userId), application.email.containsIgnoreCase(keyword))
            .exists(),
    )
}
