package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

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

    fun deleteAllByAttemptId(attemptId: Long)
}
