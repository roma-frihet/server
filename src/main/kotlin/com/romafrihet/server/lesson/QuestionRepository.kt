package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.projection.LessonQuestionCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface QuestionRepository : JpaRepository<Question, Long> {
    fun findByIdAndCourseUserId(id: Long, userId: UUID): Question?

    fun existsByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun existsByCourseId(courseId: Long): Boolean

    @Query("select q.imageId from Question q where q.imageId in :imageIds")
    fun findQuestionImageIds(imageIds: Collection<Long>): List<Long>

    @Query("select o.imageId from QuestionOption o where o.imageId in :imageIds")
    fun findOptionImageIds(imageIds: Collection<Long>): List<Long>

    @Query(
        """
        select distinct q from Question q
        left join fetch q.options
        where q.course.id = :courseId and q.lessonNumber = :lessonNumber
        order by q.id
        """,
    )
    fun findAllWithOptions(courseId: Long, lessonNumber: Int): List<Question>

    @Query("select q from Question q left join fetch q.options where q.id = :id and q.course.userId = :userId")
    fun findWithOptions(id: Long, userId: UUID): Question?

    @Query(
        """
        select new com.romafrihet.server.lesson.projection.LessonQuestionCountProjection(q.lessonNumber, count(q))
        from Question q
        where q.course.id = :courseId
        group by q.lessonNumber
        """,
    )
    fun findQuestionCounts(courseId: Long): List<LessonQuestionCountProjection>
}

/** 다른 교사의 문제는 존재 자체를 알리지 않도록 404로 응답한다. */
fun QuestionRepository.getOwned(id: Long, userId: UUID): Question =
    findByIdAndCourseUserId(id, userId) ?: throw BusinessException(ErrorCode.NOT_FOUND)
