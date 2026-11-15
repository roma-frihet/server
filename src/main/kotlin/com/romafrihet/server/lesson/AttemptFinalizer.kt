package com.romafrihet.server.lesson

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

/**
 * 제한 시간이 지났는데 확정되지 않은 응답을 확정한다.
 * 학생이 브라우저를 닫고 돌아오지 않아도 점수가 맞도록, 읽기 전에 먼저 부른다.
 */
@Component
class AttemptFinalizer(
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val attemptProperties: AttemptProperties,
    private val clock: Clock,
) {
    /** questionCounts는 차시 번호별 문제 수다. 마지막 문제가 확정되면 응시가 끝난다. */
    fun finalizeExpired(attempts: List<Attempt>, questionCounts: Map<Int, Int>) {
        if (attempts.isEmpty()) return
        val now = LocalDateTime.now(clock)
        val byId = attempts.associateBy { requireNotNull(it.id) }
        attemptAnswerRepository
            .findAllByAttemptIdInAndIsFinalizedFalseAndDeadlineLessThan(
                byId.keys,
                now.minusSeconds(attemptProperties.graceSeconds),
            ).forEach { answer ->
                answer.finalizeWith(
                    optionId = answer.optionId,
                    answerText = answer.answerText,
                    byTimeout = true,
                    now = now,
                )
                val attempt = byId.getValue(requireNotNull(answer.attempt.id))
                attempt.completeQuestion(questionCounts[attempt.lessonNumber] ?: 0, now)
            }
    }

    fun isOverDeadline(answer: AttemptAnswer, now: LocalDateTime): Boolean =
        now.isAfter(answer.deadline.plusSeconds(attemptProperties.graceSeconds))
}
