package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
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

    fun deleteAllByAttemptId(attemptId: Long)
}
