package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.event.QuestionImagesRemovedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 문제나 보기를 지운 뒤 아무도 쓰지 않게 된 이미지를 치운다. */
@Service
class QuestionImageCleaner(
    private val questionRepository: QuestionRepository,
    private val questionImageRepository: QuestionImageRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /** 같은 이미지를 다른 문제나 보기가 쓰고 있을 수 있어, 변경을 반영한 뒤 참조가 없는 것만 지운다. */
    @Transactional
    fun removeUnused(imageIds: List<Long>) {
        if (imageIds.isEmpty()) return
        questionRepository.flush()
        val used =
            questionRepository.findQuestionImageIds(imageIds) + questionRepository.findOptionImageIds(imageIds)
        val unused = questionImageRepository.findAllById(imageIds.toSet() - used.toSet())
        questionImageRepository.deleteAllInBatch(unused)
        if (unused.isNotEmpty()) eventPublisher.publishEvent(QuestionImagesRemovedEvent(unused.map { it.storagePath }))
    }
}
