package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface LessonSubmissionRepository :
    JpaRepository<LessonSubmission, Long>,
    LessonSubmissionRepositoryExtension {
    fun existsByClassroomIdAndLessonNumberAndUserId(classroomId: Long, lessonNumber: Int, userId: UUID): Boolean

    fun existsByClassroomCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<LessonSubmission>

    @Query(
        """
        select distinct s from LessonSubmission s
        left join fetch s.answers
        where s.classroom.id = :classroomId and s.lessonNumber = :lessonNumber and s.userId = :userId
        """,
    )
    fun findWithAnswers(classroomId: Long, lessonNumber: Int, userId: UUID): LessonSubmission?
}
