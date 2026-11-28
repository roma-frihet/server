package com.romafrihet.server.lesson

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

/** 벌크 삭제라 영속성 컨텍스트가 모르는 채로 지워진다. 뒤이어 엔티티를 읽는 쪽이 옛 값을 보지 않도록 비운다. */
class AttemptAnswerRepositoryExtensionImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) : AttemptAnswerRepositoryExtension {
    private val answer = QAttemptAnswer.attemptAnswer

    override fun deleteAllByQuestionId(questionId: Long) {
        entityManager.flush()
        queryFactory.delete(answer).where(answer.question.id.eq(questionId)).execute()
        entityManager.clear()
    }
}
