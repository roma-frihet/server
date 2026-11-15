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
import java.time.Duration
import java.time.LocalDateTime

/**
 * 문제 하나의 응답이다. 학생이 그 문제를 시작할 때 만들어진다.
 * deadline은 만든 뒤 바뀌지 않고, 다음 문제의 시간은 그 문제를 시작할 때까지 흐르지 않는다.
 */
@Entity
@Table(name = "attempt_answers")
class AttemptAnswer private constructor(
    attempt: Attempt,
    question: Question,
    questionIndex: Int,
    timeLimitSec: Int,
    startedAt: LocalDateTime,
    deadline: LocalDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attempt_answers_seq")
    @SequenceGenerator(name = "attempt_answers_seq", sequenceName = "attempt_answers_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, updatable = false)
    var attempt: Attempt = attempt
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    var question: Question = question
        protected set

    @Column(name = "question_index", nullable = false, updatable = false)
    var questionIndex: Int = questionIndex
        protected set

    /** 문제의 제한 시간이 나중에 바뀌어도 이 응시의 기준은 시작 시점 값이다. */
    @Column(name = "time_limit_sec", nullable = false, updatable = false)
    var timeLimitSec: Int = timeLimitSec
        protected set

    @Column(name = "started_at", nullable = false, updatable = false)
    var startedAt: LocalDateTime = startedAt
        protected set

    @Column(nullable = false, updatable = false)
    var deadline: LocalDateTime = deadline
        protected set

    @Column(name = "question_option_id")
    var optionId: Long? = null
        protected set

    @Column(name = "answer_text", columnDefinition = "text")
    var answerText: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "final_result", nullable = false, length = 10)
    var finalResult: QuestionResult = QuestionResult.NONE
        protected set

    @Column(name = "is_finalized", nullable = false)
    var isFinalized: Boolean = false
        protected set

    @Column(name = "by_timeout", nullable = false)
    var byTimeout: Boolean = false
        protected set

    @Column(name = "is_overridden", nullable = false)
    var isOverridden: Boolean = false
        protected set

    @Column(name = "finalized_at")
    var finalizedAt: LocalDateTime? = null
        protected set

    fun remainingMillis(now: LocalDateTime): Long = Duration.between(now, deadline).toMillis().coerceAtLeast(0)

    fun hasAnswer(): Boolean = optionId != null || !answerText.isNullOrBlank()

    fun saveDraft(optionId: Long?, answerText: String?) {
        this.optionId = optionId
        this.answerText = answerText
    }

    /** 확정은 한 번만 일어난다. 이미 확정된 문제에 다시 제출해도 결과가 바뀌지 않는다. */
    fun finalizeWith(optionId: Long?, answerText: String?, byTimeout: Boolean, now: LocalDateTime) {
        if (isFinalized) return
        saveDraft(optionId, answerText)
        finalResult =
            when {
                !hasAnswer() -> QuestionResult.NONE
                question.isCorrectAnswer(optionId, answerText) -> QuestionResult.RIGHT
                else -> QuestionResult.WRONG
            }
        this.byTimeout = byTimeout
        isFinalized = true
        finalizedAt = now
    }

    /** 교사가 결과를 덮어쓴다. 원래 결과는 ResultOverride에 남긴다. */
    fun overrideResult(result: QuestionResult) {
        finalResult = result
        isOverridden = true
    }

    companion object {
        fun create(attempt: Attempt, question: Question, questionIndex: Int, startedAt: LocalDateTime): AttemptAnswer =
            AttemptAnswer(
                attempt = attempt,
                question = question,
                questionIndex = questionIndex,
                timeLimitSec = question.timeLimitSec,
                startedAt = startedAt,
                deadline = startedAt.plusSeconds(question.timeLimitSec.toLong()),
            )
    }
}
