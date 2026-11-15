package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository

interface ResultOverrideRepository : JpaRepository<ResultOverride, Long> {
    fun deleteAllByAttemptAnswerAttemptId(attemptId: Long)
}
