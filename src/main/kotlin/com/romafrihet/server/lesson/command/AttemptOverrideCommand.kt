package com.romafrihet.server.lesson.command

import com.romafrihet.server.lesson.QuestionResult
import java.util.UUID

class AttemptOverrideCommand private constructor() {
    data class Update(val userId: UUID, val attemptAnswerId: Long, val result: QuestionResult)
}
