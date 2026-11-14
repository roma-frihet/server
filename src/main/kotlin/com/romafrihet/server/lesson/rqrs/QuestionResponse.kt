package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.dto.QuestionImageDto
import com.romafrihet.server.lesson.dto.QuestionOptionDto
import io.swagger.v3.oas.annotations.media.Schema

class QuestionResponse private constructor() {
    @Schema(name = "QuestionResponseOption")
    data class Option(
        val id: Long,
        val displayOrder: Int,
        val content: String?,
        val imageId: Long?,
        val imageUrl: String?,
        val isCorrect: Boolean,
    ) {
        companion object {
            fun of(dto: QuestionOptionDto) = Option(
                id = dto.id,
                displayOrder = dto.displayOrder,
                content = dto.content,
                imageId = dto.imageId,
                imageUrl = dto.imageUrl,
                isCorrect = dto.isCorrect,
            )
        }
    }

    @Schema(name = "QuestionResponseResult")
    data class Result(
        val id: Long,
        val lessonNumber: Int,
        val type: QuestionType,
        val content: String?,
        val imageId: Long?,
        val imageUrl: String?,
        val options: List<Option>,
    ) {
        companion object {
            fun of(dto: QuestionDto) = Result(
                id = dto.id,
                lessonNumber = dto.lessonNumber,
                type = dto.type,
                content = dto.content,
                imageId = dto.imageId,
                imageUrl = dto.imageUrl,
                options = dto.options.map(Option::of),
            )
        }
    }

    @Schema(name = "QuestionResponseIdResult")
    data class IdResult(val id: Long)

    @Schema(name = "QuestionResponseImageResult")
    data class ImageResult(val id: Long, val url: String?) {
        companion object {
            fun of(dto: QuestionImageDto) = ImageResult(id = dto.id, url = dto.url)
        }
    }

    /** 학생에게는 정답 여부를 내리지 않는다. */
    @Schema(name = "QuestionResponseStudentOption")
    data class StudentOption(val id: Long, val displayOrder: Int, val content: String?, val imageUrl: String?) {
        companion object {
            fun of(dto: QuestionOptionDto) = StudentOption(
                id = dto.id,
                displayOrder = dto.displayOrder,
                content = dto.content,
                imageUrl = dto.imageUrl,
            )
        }
    }

    @Schema(name = "QuestionResponseStudentResult")
    data class StudentResult(
        val id: Long,
        val type: QuestionType,
        val content: String?,
        val imageUrl: String?,
        val options: List<StudentOption>,
    ) {
        companion object {
            /** 주관식 옵션은 정답 자체이므로 비워서 내린다. */
            fun of(dto: QuestionDto) = StudentResult(
                id = dto.id,
                type = dto.type,
                content = dto.content,
                imageUrl = dto.imageUrl,
                options =
                when (dto.type) {
                    QuestionType.MULTIPLE_CHOICE -> dto.options.map(StudentOption::of)
                    QuestionType.SHORT_ANSWER -> emptyList()
                },
            )
        }
    }
}
