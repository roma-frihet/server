package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface LessonOpeningRepository : JpaRepository<LessonOpening, Long> {
    fun findByClassroomIdAndLessonNumber(classroomId: Long, lessonNumber: Int): LessonOpening?

    fun findByClassroomIdAndClosedAtIsNull(classroomId: Long): LessonOpening?

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<LessonOpening>

    fun findAllByClassroomIdInAndLessonNumber(classroomIds: Collection<Long>, lessonNumber: Int): List<LessonOpening>

    fun findAllByClassroomIdOrderByLessonNumberAsc(classroomId: Long): List<LessonOpening>

    fun existsByClassroomId(classroomId: Long): Boolean

    fun existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId: Long, lessonNumber: Int): Boolean

    fun existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(courseId: Long, lessonNumber: Int): Boolean

    /** 반이나 차시를 지우면 연 기록도 함께 사라진다. 이 기록이 남아 있으면 반을 지울 수 없었다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LessonOpening o where o.classroom.id in :classroomIds")
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LessonOpening o where o.classroom.course.id = :courseId and o.lessonNumber = :lessonNumber")
    fun deleteAllByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int)
}
