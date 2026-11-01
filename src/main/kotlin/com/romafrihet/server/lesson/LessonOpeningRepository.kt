package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository

interface LessonOpeningRepository : JpaRepository<LessonOpening, Long> {
    fun findByClassroomIdAndLessonNumber(classroomId: Long, lessonNumber: Int): LessonOpening?

    fun findByClassroomIdAndClosedAtIsNull(classroomId: Long): LessonOpening?

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<LessonOpening>

    fun existsByClassroomId(classroomId: Long): Boolean

    fun existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId: Long, lessonNumber: Int): Boolean

    fun existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(courseId: Long, lessonNumber: Int): Boolean
}
