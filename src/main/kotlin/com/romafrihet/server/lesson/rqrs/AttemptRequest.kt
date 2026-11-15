package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.command.AttemptCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

class AttemptRequest private constructor() {
    /** 객관식은 optionId, 주관식은 text를 쓴다. 임시 저장은 둘 다 비워도 된다. */
    @Schema(name = "AttemptRequestAnswer")
    data class Answer(val optionId: Long? = null, val text: String? = null) {
        fun toCommand(userId: UUID, classroomId: Long, lessonNumber: Int, questionIndex: Int) = AttemptCommand.Answer(
            userId = userId,
            classroomId = classroomId,
            lessonNumber = lessonNumber,
            questionIndex = questionIndex,
            optionId = optionId,
            text = text,
        )
    }
}
