package com.romafrihet.server.lesson.command

import com.romafrihet.server.lesson.QuestionResult
import java.util.UUID

class AttemptOverrideCommand private constructor() {
    data class Update(val userId: UUID, val attemptAnswerId: Long, val result: QuestionResult)

    /** 한 반·차시·문제에서 같은 답을 쓴 응답을 한 번에 바꾼다. */
    data class ByAnswer(
        val userId: UUID,
        val classroomId: Long,
        val lessonNumber: Int,
        val questionId: Long,
        val answerText: String,
        val result: QuestionResult,
    )
}
