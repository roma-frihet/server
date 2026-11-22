package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.projection.AnswerCountProjection
import com.romafrihet.server.lesson.projection.ClassroomResponseCountProjection
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface AttemptAnswerRepository : JpaRepository<AttemptAnswer, Long> {
    fun findByAttemptIdAndQuestionIndex(attemptId: Long, questionIndex: Int): AttemptAnswer?

    @EntityGraph(attributePaths = ["question"])
    fun findAllByAttemptIdOrderByQuestionIndexAsc(attemptId: Long): List<AttemptAnswer>

    /** 시간이 지났는데 아직 확정되지 않은 응답이다. 읽기 전에 이것부터 확정한다. */
    @EntityGraph(attributePaths = ["question"])
    fun findAllByAttemptIdInAndIsFinalizedFalseAndDeadlineLessThan(
        attemptIds: Collection<Long>,
        threshold: LocalDateTime,
    ): List<AttemptAnswer>

    fun findByIdAndAttemptClassroomCourseUserId(id: Long, userId: UUID): AttemptAnswer?

    /** 마감한 차시에서 맞히지 못한 문제다. 보기는 BatchSize에 맡긴다. */
    @Query(
        """
        select a from AttemptAnswer a
        join fetch a.question
        join fetch a.attempt t
        where t.classroom.id = :classroomId
          and t.userId = :userId
          and t.lessonNumber in :lessonNumbers
          and a.finalResult <> com.romafrihet.server.lesson.QuestionResult.RIGHT
        order by t.lessonNumber desc, a.questionIndex asc
        """,
    )
    fun findWrongAnswers(classroomId: Long, userId: UUID, lessonNumbers: Collection<Int>): List<AttemptAnswer>

    /** 문제별 분포를 한 번에 센다. 확정되지 않은 응답은 학생이 아직 푸는 중이라 넣지 않는다. */
    @Query(
        """
        select new com.romafrihet.server.lesson.projection.AnswerCountProjection(
            t.classroom.id, a.question.id, a.optionId, a.answerText, a.finalResult, count(a)
        )
        from AttemptAnswer a
        join a.attempt t
        where t.classroom.id in :classroomIds
          and t.lessonNumber = :lessonNumber
          and a.isFinalized = true
        group by t.classroom.id, a.question.id, a.optionId, a.answerText, a.finalResult
        """,
    )
    fun findAnswerCounts(classroomIds: Collection<Long>, lessonNumber: Int): List<AnswerCountProjection>

    @Query(
        """
        select new com.romafrihet.server.lesson.projection.ClassroomResponseCountProjection(
            t.classroom.id, count(distinct t.userId)
        )
        from AttemptAnswer a
        join a.attempt t
        where t.classroom.id in :classroomIds
          and t.lessonNumber = :lessonNumber
          and a.finalResult <> com.romafrihet.server.lesson.QuestionResult.NONE
        group by t.classroom.id
        """,
    )
    fun findResponseCounts(classroomIds: Collection<Long>, lessonNumber: Int): List<ClassroomResponseCountProjection>

    fun deleteAllByAttemptId(attemptId: Long)

    /** 반이나 차시를 통째로 지울 때 쓴다. 응시 하나씩 돌면 인원수만큼 쿼리가 나가므로 한 번에 지운다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AttemptAnswer a where a.attempt.classroom.id in :classroomIds")
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from AttemptAnswer a
        where a.attempt.classroom.course.id = :courseId and a.attempt.lessonNumber = :lessonNumber
        """,
    )
    fun deleteAllByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int)
}
