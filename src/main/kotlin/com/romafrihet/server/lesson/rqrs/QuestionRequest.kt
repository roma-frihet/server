package com.romafrihet.server.lesson.rqrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.QuestionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

private const val MIN_TIME_LIMIT_SEC = 1L
private const val MAX_TIME_LIMIT_SEC = 300L
private const val DEFAULT_TIME_LIMIT_SEC = 30
private const val MAX_COPY_COUNT = 100

class QuestionRequest private constructor() {
    @Schema(name = "QuestionRequestOption")
    data class Option(
        val content: String?,
        val imageId: Long?,
        @field:NotNull
        @get:JsonProperty("isCorrect")
        val isCorrect: Boolean?,
    ) {
        fun toCommand() = QuestionCommand.Option(
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            isCorrect = requireNotNull(isCorrect),
        )
    }

    @Schema(name = "QuestionRequestSave")
    data class Save(
        @field:NotNull
        val type: QuestionType?,
        val content: String?,
        val imageId: Long?,
        @field:Min(MIN_TIME_LIMIT_SEC)
        @field:Max(MAX_TIME_LIMIT_SEC)
        val timeLimitSec: Int? = null,
        val explanation: String? = null,
        @field:NotEmpty
        @field:Valid
        val options: List<Option>?,
    ) {
        /** 값을 주지 않으면 30초로 둔다. */
        private fun timeLimit() = timeLimitSec ?: DEFAULT_TIME_LIMIT_SEC

        /** 빈 해설은 없는 것으로 저장한다. */
        private fun explanationOrNull() = explanation?.trim()?.ifEmpty { null }

        fun toCreateCommand(userId: UUID, courseId: Long, lessonNumber: Int) = QuestionCommand.Create(
            userId = userId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            timeLimitSec = timeLimit(),
            explanation = explanationOrNull(),
            options = requireNotNull(options).map(Option::toCommand),
        )

        fun toUpdateCommand(userId: UUID, questionId: Long) = QuestionCommand.Update(
            userId = userId,
            questionId = questionId,
            type = requireNotNull(type),
            content = content?.trim()?.ifEmpty { null },
            imageId = imageId,
            timeLimitSec = timeLimit(),
            explanation = explanationOrNull(),
            options = requireNotNull(options).map(Option::toCommand),
        )
    }

    @Schema(name = "QuestionRequestCopy")
    data class Copy(
        @field:Schema(description = "복사해 넣을 수업. 같은 수업이어도 된다")
        @field:NotNull
        val targetCourseId: Long?,
        @field:Schema(description = "복사해 넣을 차시. 비우면 대상 수업에 원본 제목으로 새 차시를 만든다")
        @field:Positive
        val targetLessonNumber: Int? = null,
        @field:Schema(description = "복사할 문제. 비우면 차시의 모든 문제")
        @field:Size(max = MAX_COPY_COUNT)
        val questionIds: List<Long>? = null,
    ) {
        fun toCommand(userId: UUID, courseId: Long, lessonNumber: Int) = QuestionCommand.Copy(
            userId = userId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            targetCourseId = requireNotNull(targetCourseId),
            targetLessonNumber = targetLessonNumber,
            questionIds = questionIds.orEmpty(),
        )
    }
}
