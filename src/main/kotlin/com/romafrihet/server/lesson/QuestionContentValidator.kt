package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.QuestionCommand
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QuestionContentValidator(private val questionImageRepository: QuestionImageRepository) {
    fun validate(
        userId: UUID,
        type: QuestionType,
        content: String?,
        imageId: Long?,
        options: List<QuestionCommand.Option>,
    ) {
        assertCheck(!content.isNullOrBlank() || imageId != null) { ErrorCode.QUESTION_CONTENT_REQUIRED }
        validateImages(userId, (listOfNotNull(imageId) + options.mapNotNull { it.imageId }).toSet())
        assertCheck(isValidOptions(type, options)) { ErrorCode.QUESTION_OPTIONS_INVALID }
    }

    /** 다른 교사의 파일을 연결하지 못하도록 자기가 올린 이미지만 받는다. */
    private fun validateImages(userId: UUID, imageIds: Set<Long>) {
        if (imageIds.isEmpty()) return
        assertCheck(questionImageRepository.countByIdInAndUserId(imageIds, userId) == imageIds.size.toLong()) {
            ErrorCode.QUESTION_IMAGE_NOT_USABLE
        }
    }

    private fun isValidOptions(type: QuestionType, options: List<QuestionCommand.Option>): Boolean = when (type) {
        QuestionType.MULTIPLE_CHOICE ->
            options.size in CHOICE_COUNT_RANGE &&
                options.count { it.isCorrect } == 1 &&
                options.all { !it.content.isNullOrBlank() || it.imageId != null }

        QuestionType.SHORT_ANSWER ->
            options.isNotEmpty() &&
                options.all { it.isCorrect && !it.content.isNullOrBlank() && it.imageId == null }
    }

    private companion object {
        val CHOICE_COUNT_RANGE = 2..5
    }
}
