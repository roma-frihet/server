package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.projection.AttemptScoreProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AttemptRepository :
    JpaRepository<Attempt, Long>,
    AttemptRepositoryExtension {
    fun findByClassroomIdAndLessonNumberAndUserId(classroomId: Long, lessonNumber: Int, userId: UUID): Attempt?

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<Attempt>

    fun findAllByClassroomCourseId(courseId: Long): List<Attempt>

    fun findByClassroomIdAndUserId(classroomId: Long, userId: UUID): List<Attempt>

    fun existsByClassroomCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int): Boolean

    fun deleteAllByClassroomIdAndUserId(classroomId: Long, userId: UUID)

    /** 반이나 차시를 통째로 지울 때 쓴다. 응답과 재채점을 먼저 지운 뒤에 불러야 한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Attempt a where a.classroom.id in :classroomIds")
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Attempt a where a.classroom.course.id = :courseId and a.lessonNumber = :lessonNumber")
    fun deleteAllByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int)

    /** 점수의 바탕이 되는 (학생 x 차시) 격자다. 여기서 반 평균과 추이가 모두 나온다. */
    @Query(
        """
        select new com.romafrihet.server.lesson.projection.AttemptScoreProjection(
            a.classroom.id,
            a.userId,
            a.lessonNumber,
            count(case when r.finalResult = com.romafrihet.server.lesson.QuestionResult.RIGHT then 1 end),
            count(case when r.finalResult <> com.romafrihet.server.lesson.QuestionResult.NONE then 1 end))
        from AttemptAnswer r
        join r.attempt a
        where a.classroom.id in :classroomIds
        group by a.classroom.id, a.userId, a.lessonNumber
        """,
    )
    fun findScores(classroomIds: Collection<Long>): List<AttemptScoreProjection>
}
