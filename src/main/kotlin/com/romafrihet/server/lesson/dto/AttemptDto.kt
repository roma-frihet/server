package com.romafrihet.server.lesson.dto

import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.QuestionResult

/**
 * 응시 상태다. current는 푸는 중일 때만, last는 직전에 확정된 문제가 있을 때만 담는다.
 * 아직 시작하지 않은 문제는 개수와 제한 시간만 알려준다.
 */
data class AttemptDto(
    val phase: AttemptPhase,
    val total: Int,
    val nextIndex: Int,
    val nextTimeLimitSec: Int?,
    val correctCount: Int?,
    val current: CurrentQuestionDto?,
    val last: FinalizedAnswerDto?,
)

data class CurrentQuestionDto(
    val questionIndex: Int,
    val question: QuestionDto,
    val remainingMillis: Long,
    val draftOptionId: Long?,
    val draftText: String?,
)

/** 확정된 뒤에만 만든다. 정답과 해설은 여기에서만 나간다. */
data class FinalizedAnswerDto(
    val questionIndex: Int,
    val result: QuestionResult,
    val byTimeout: Boolean,
    val selectedOptionId: Long?,
    val answerText: String?,
    val correctOptionId: Long?,
    val correctAnswer: String?,
    val explanation: String?,
)
