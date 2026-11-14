package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.dto.QuestionOptionDto
import com.romafrihet.server.lesson.rqrs.QuestionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QuestionResponseTest {
    private fun dto(type: QuestionType) = QuestionDto(
        id = 1,
        lessonNumber = 1,
        type = type,
        content = "문제",
        imageId = null,
        imageUrl = null,
        options = listOf(QuestionOptionDto(2, 0, "정답", null, null, isCorrect = true)),
    )

    @Test
    fun `학생 객관식 응답은 보기를 담는다`() {
        val result = QuestionResponse.StudentResult.of(dto(QuestionType.MULTIPLE_CHOICE))

        assertThat(result.options.map { it.content }).containsExactly("정답")
    }

    @Test
    fun `학생 주관식 응답은 인정 답안을 내리지 않는다`() {
        val result = QuestionResponse.StudentResult.of(dto(QuestionType.SHORT_ANSWER))

        assertThat(result.options).isEmpty()
    }
}
