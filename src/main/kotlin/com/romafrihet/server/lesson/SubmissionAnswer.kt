package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "submission_answers")
class SubmissionAnswer private constructor(
    submission: LessonSubmission,
    question: Question,
    optionId: Long?,
    answerText: String?,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "submission_answers_seq")
    @SequenceGenerator(name = "submission_answers_seq", sequenceName = "submission_answers_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_submission_id", nullable = false, updatable = false)
    var submission: LessonSubmission = submission
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    var question: Question = question
        protected set

    @Column(name = "question_option_id", updatable = false)
    var optionId: Long? = optionId
        protected set

    @Column(name = "answer_text", columnDefinition = "text", updatable = false)
    var answerText: String? = answerText
        protected set

    @Column(name = "is_correct", nullable = false, updatable = false)
    var isCorrect: Boolean = question.isCorrectAnswer(optionId, answerText)
        protected set

    companion object {
        fun create(
            submission: LessonSubmission,
            question: Question,
            optionId: Long?,
            answerText: String?,
        ): SubmissionAnswer = SubmissionAnswer(submission, question, optionId, answerText)
    }
}
