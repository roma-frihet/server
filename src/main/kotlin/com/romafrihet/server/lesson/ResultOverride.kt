package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

/** 교사가 결과를 고친 이력이다. 집계는 이 표를 읽지 않고 AttemptAnswer.finalResult만 센다. */
@Entity
@Table(name = "result_overrides")
class ResultOverride private constructor(
    attemptAnswer: AttemptAnswer,
    userId: UUID,
    beforeResult: QuestionResult,
    afterResult: QuestionResult,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_overrides_seq")
    @SequenceGenerator(name = "result_overrides_seq", sequenceName = "result_overrides_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_answer_id", nullable = false, updatable = false)
    var attemptAnswer: AttemptAnswer = attemptAnswer
        protected set

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "before_result", nullable = false, updatable = false, length = 10)
    var beforeResult: QuestionResult = beforeResult
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "after_result", nullable = false, updatable = false, length = 10)
    var afterResult: QuestionResult = afterResult
        protected set

    companion object {
        fun create(attemptAnswer: AttemptAnswer, userId: UUID, afterResult: QuestionResult): ResultOverride =
            ResultOverride(
                attemptAnswer = attemptAnswer,
                userId = userId,
                beforeResult = attemptAnswer.finalResult,
                afterResult = afterResult,
            )
    }
}
