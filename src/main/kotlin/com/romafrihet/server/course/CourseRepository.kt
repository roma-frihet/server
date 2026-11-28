package com.romafrihet.server.course

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.projection.CourseCountProjection
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CourseRepository : JpaRepository<Course, Long> {
    fun findByIdAndUserId(id: Long, userId: UUID): Course?

    fun existsByUserIdAndGradeAndSubjectId(userId: UUID, grade: Int, subjectId: Long): Boolean

    @EntityGraph(attributePaths = ["school", "subject"])
    fun findAllByUserIdOrderByGradeAscSubjectDisplayOrderAsc(userId: UUID): List<Course>

    @Query(
        """
        select new com.romafrihet.server.course.projection.CourseCountProjection(c.userId, count(c))
        from Course c
        where c.userId in :userIds
        group by c.userId
        """,
    )
    fun findCourseCounts(userIds: Collection<UUID>): List<CourseCountProjection>
}

/** 다른 교사의 수업은 존재 자체를 알리지 않도록 404로 응답한다. */
fun CourseRepository.getOwned(id: Long, userId: UUID): Course =
    findByIdAndUserId(id, userId) ?: throw BusinessException(ErrorCode.NOT_FOUND)
