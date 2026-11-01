package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
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
}

fun LessonRepository.getByNumber(courseId: Long, lessonNumber: Int): Lesson =
    findByCourseIdAndLessonNumber(courseId, lessonNumber) ?: throw BusinessException(ErrorCode.NOT_FOUND)
