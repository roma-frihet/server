package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.course.Course
import com.romafrihet.server.lesson.command.QuestionCommand
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "questions")
class Question private constructor(
    course: Course,
    lessonNumber: Int,
    type: QuestionType,
    content: String?,
    imageId: Long?,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questions_seq")
    @SequenceGenerator(name = "questions_seq", sequenceName = "questions_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    var course: Course = course
        protected set

    @Column(name = "lesson_number", nullable = false, updatable = false)
    var lessonNumber: Int = lessonNumber
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: QuestionType = type
        protected set

    @Column(columnDefinition = "text")
    var content: String? = content
        protected set

    @Column(name = "question_image_id")
    var imageId: Long? = imageId
        protected set

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    val options: MutableList<QuestionOption> = mutableListOf()

    /** 주관식은 공백과 대소문자를 무시하고 인정 답안과 비교한다. */
    fun isCorrectAnswer(optionId: Long?, text: String?): Boolean = when (type) {
        QuestionType.MULTIPLE_CHOICE -> options.any { it.id == optionId && it.isCorrect }
        QuestionType.SHORT_ANSWER -> text != null && options.any { it.content?.let(::normalize) == normalize(text) }
    }

    private fun normalize(value: String): String = value.filterNot(Char::isWhitespace).lowercase()

    fun imageIds(): List<Long> = listOfNotNull(imageId) + options.mapNotNull { it.imageId }

    fun update(command: QuestionCommand.Update) {
        type = command.type
        content = command.content
        imageId = command.imageId
        replaceOptions(command.options)
    }

    /**
     * Hibernate는 삭제보다 삽입을 먼저 실행하므로 전부 지우고 새로 넣으면
     * (question_id, display_order) 유일 제약에 걸린다. 같은 순번의 행은 재사용한다.
     */
    private fun replaceOptions(inputs: List<QuestionCommand.Option>) {
        options.zip(inputs).forEach { (option, input) -> option.update(input) }
        while (options.size > inputs.size) options.removeLast()
        inputs.drop(options.size).forEach(::addOption)
    }

    private fun addOption(input: QuestionCommand.Option) {
        options.add(QuestionOption.create(this, options.size, input))
    }

    companion object {
        fun create(course: Course, command: QuestionCommand.Create): Question = Question(
            course = course,
            lessonNumber = command.lessonNumber,
            type = command.type,
            content = command.content,
            imageId = command.imageId,
        ).apply { command.options.forEach(::addOption) }
    }
}
