package com.romafrihet.server.lesson.rqrs

import io.swagger.v3.oas.annotations.media.Schema

class AttemptOverrideResponse private constructor() {
    @Schema(name = "AttemptOverrideResponseByAnswer")
    data class ByAnswer(
        @field:Schema(description = "결과가 바뀐 응답 수. 이미 그 결과였던 응답은 세지 않는다")
        val changedCount: Int,
    )
}
