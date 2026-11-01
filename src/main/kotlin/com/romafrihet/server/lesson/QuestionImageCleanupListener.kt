package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.lesson.event.QuestionImagesRemovedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * common이 lesson 이벤트를 참조하면 패키지 순환이 생겨 common/listener가 아닌 이곳에 둔다.
 * 파일 삭제가 실패해도 이미 커밋된 문제 변경은 되돌리지 않는다.
 */
@Component
class QuestionImageCleanupListener(private val questionImageService: QuestionImageService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: QuestionImagesRemovedEvent) {
        try {
            questionImageService.deleteAll(event.paths)
        } catch (e: BusinessException) {
            logger.warn("문제 이미지 {}건 삭제 실패", event.paths.size, e)
        }
    }
}
