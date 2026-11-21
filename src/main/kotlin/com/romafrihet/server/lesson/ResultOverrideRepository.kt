package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ResultOverrideRepository : JpaRepository<ResultOverride, Long> {
    fun deleteAllByAttemptAnswerAttemptId(attemptId: Long)

    /**
     * 반을 통째로 지울 때 쓴다. 응시 하나씩 돌면 반 인원만큼 쿼리가 나가므로 한 번에 지운다.
     * JPQL 벌크 삭제는 조인을 못 쓰기 때문에 서브쿼리로 묶는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from ResultOverride r
        where r.attemptAnswer.id in (
            select a.id from AttemptAnswer a where a.attempt.classroom.id in :classroomIds
        )
        """,
    )
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from ResultOverride r
        where r.attemptAnswer.id in (
            select a.id from AttemptAnswer a
            where a.attempt.classroom.course.id = :courseId and a.attempt.lessonNumber = :lessonNumber
        )
        """,
    )
    fun deleteAllByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int)
}
