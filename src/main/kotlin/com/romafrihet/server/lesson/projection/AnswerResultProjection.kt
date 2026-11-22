package com.romafrihet.server.lesson.projection

import com.romafrihet.server.lesson.QuestionResult
import java.util.UUID

/** 학생 한 명이 문제 하나에서 받은 확정 결과다. 정오표와 문제별 정답률을 만든다. */
data class AnswerResultProjection(
    val classroomId: Long,
    val userId: UUID,
    val questionId: Long,
    val finalResult: QuestionResult,
)
