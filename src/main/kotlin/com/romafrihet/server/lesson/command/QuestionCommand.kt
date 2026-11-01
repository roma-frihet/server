package com.romafrihet.server.lesson.command

import com.romafrihet.server.lesson.QuestionType
import java.util.UUID

class QuestionCommand private constructor() {
    data class Option(val content: String?, val imageId: Long?, val isCorrect: Boolean)

    data class Create(
        val userId: UUID,
        val courseId: Long,
        val lessonNumber: Int,
        val type: QuestionType,
        val content: String?,
        val imageId: Long?,
        val options: List<Option>,
    )

    data class Update(
        val userId: UUID,
        val questionId: Long,
        val type: QuestionType,
        val content: String?,
        val imageId: Long?,
        val options: List<Option>,
    )

    data class Query(val userId: UUID, val courseId: Long, val lessonNumber: Int)

    data class Get(val userId: UUID, val questionId: Long)

    data class Delete(val userId: UUID, val questionId: Long)

    data class StudentQuery(val userId: UUID, val classroomId: Long, val lessonNumber: Int)

    data class UploadImage(val userId: UUID, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean =
            this === other || (other is UploadImage && userId == other.userId && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = 31 * userId.hashCode() + bytes.contentHashCode()
    }
}
