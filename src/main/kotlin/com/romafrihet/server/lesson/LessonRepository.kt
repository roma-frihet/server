package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.projection.CourseLessonProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LessonRepository : JpaRepository<Lesson, Long> {
    fun existsByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun findByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Lesson?

    fun findAllByCourseIdOrderByLessonNumberAsc(courseId: Long): List<Lesson>

    fun findAllByCourseIdIn(courseIds: Collection<Long>): List<Lesson>

    fun deleteAllByCourseId(courseId: Long)

    @Query("select max(l.lessonNumber) from Lesson l where l.course.id = :courseId")
    fun findMaxLessonNumber(courseId: Long): Int?

    /** 문제가 하나 이상 있는 차시다. 예약 열기는 문제가 없는 차시를 건너뛴다. */
    @Query(
        """
        select distinct new com.romafrihet.server.lesson.projection.CourseLessonProjection(q.course.id, q.lessonNumber)
        from Question q
        where q.course.id in :courseIds
        """,
    )
    fun findLessonsWithQuestions(courseIds: Collection<Long>): List<CourseLessonProjection>
}

fun LessonRepository.getByNumber(courseId: Long, lessonNumber: Int): Lesson =
    findByCourseIdAndLessonNumber(courseId, lessonNumber) ?: throw BusinessException(ErrorCode.NOT_FOUND)
