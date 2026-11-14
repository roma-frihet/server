package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.lesson.command.QuestionCommand
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

/** 객관식 보기와 주관식 인정 답안을 함께 담는다. 주관식은 모든 행이 정답이다. */
@Entity
@Table(name = "question_options")
class QuestionOption private constructor(question: Question, displayOrder: Int, option: QuestionCommand.Option) :
    BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_options_seq")
    @SequenceGenerator(name = "question_options_seq", sequenceName = "question_options_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    var question: Question = question
        protected set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        protected set

    @Column(columnDefinition = "text")
    var content: String? = option.content
        protected set

    @Column(name = "question_image_id")
    var imageId: Long? = option.imageId
        protected set

    @Column(name = "is_correct", nullable = false)
    var isCorrect: Boolean = option.isCorrect
        protected set

    fun update(option: QuestionCommand.Option) {
        content = option.content
        imageId = option.imageId
        isCorrect = option.isCorrect
    }

    companion object {
        fun create(question: Question, displayOrder: Int, option: QuestionCommand.Option): QuestionOption =
            QuestionOption(question, displayOrder, option)
    }
}
