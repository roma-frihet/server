package com.romafrihet.server.lesson.projection

import com.romafrihet.server.lesson.QuestionResult

/** 반·문제·답·결과별 확정된 응답 수다. 주관식 답은 원문으로 묶여 오므로 정규화는 읽는 쪽에서 한다. */
data class AnswerCountProjection(
    val classroomId: Long,
    val questionId: Long,
    val optionId: Long?,
    val answerText: String?,
    val finalResult: QuestionResult,
    val answerCount: Long,
)
