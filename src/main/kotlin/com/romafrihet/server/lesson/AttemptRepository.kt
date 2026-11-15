package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AttemptRepository : JpaRepository<Attempt, Long> {
    fun findByClassroomIdAndLessonNumberAndUserId(classroomId: Long, lessonNumber: Int, userId: UUID): Attempt?

    fun existsByClassroomCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun deleteAllByClassroomIdAndUserId(classroomId: Long, userId: UUID)
}
