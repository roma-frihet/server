package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.dto.QuestionOptionDto
import com.romafrihet.server.storage.StorageClient
import org.springframework.stereotype.Component

/** 엔티티가 지연 로딩 없이 읽힌 뒤, 트랜잭션 밖에서 서명 URL을 붙여 Dto로 만든다. */
@Component
class QuestionImageUrlResolver(
    private val storageClient: StorageClient,
    private val questionImageRepository: QuestionImageRepository,
) {
    fun toDtos(questions: List<Question>): List<QuestionDto> {
        val paths =
            questionImageRepository
                .findAllById(questions.flatMap { it.imageIds() }.toSet())
                .associate { requireNotNull(it.id) to it.storagePath }
        val signed = storageClient.createSignedUrls(paths.values)
        val urls = paths.mapValues { (_, path) -> signed[path] }
        return questions.map { question ->
            QuestionDto(
                id = requireNotNull(question.id),
                lessonNumber = question.lessonNumber,
                type = question.type,
                content = question.content,
                imageId = question.imageId,
                imageUrl = question.imageId?.let(urls::get),
                options =
                question.options.map { option ->
                    QuestionOptionDto(
                        id = requireNotNull(option.id),
                        displayOrder = option.displayOrder,
                        content = option.content,
                        imageId = option.imageId,
                        imageUrl = option.imageId?.let(urls::get),
                        isCorrect = option.isCorrect,
                    )
                },
            )
        }
    }
}
