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
        order by q.displayOrder
        """,
    )
    fun findAllWithOptions(courseId: Long, lessonNumber: Int): List<Question>

    /** 수업을 통째로 지울 때 쓴다. 보기는 cascade로 함께 지워지므로 엔티티로 불러온다. */
    @Query("select distinct q from Question q left join fetch q.options where q.course.id = :courseId")
    fun findAllWithOptionsByCourseId(courseId: Long): List<Question>

    fun countByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Int

    /**
     * 다음 문제가 쓸 순번. 개수로 세면 가운데 문제를 지웠을 때 이미 쓰는 번호와 겹친다.
     * (0,1,2에서 1을 지우면 0,2가 남는데 개수는 2라 2를 다시 주게 된다.)
     */
    @Query(
        "select coalesce(max(q.displayOrder) + 1, 0) from Question q " +
            "where q.course.id = :courseId and q.lessonNumber = :lessonNumber",
    )
    fun nextDisplayOrder(courseId: Long, lessonNumber: Int): Int

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
