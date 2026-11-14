package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository

interface LessonSubmissionRepository :
    JpaRepository<LessonSubmission, Long>,
    LessonSubmissionRepositoryExtension {
    fun existsByClassroomCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<LessonSubmission>
}
