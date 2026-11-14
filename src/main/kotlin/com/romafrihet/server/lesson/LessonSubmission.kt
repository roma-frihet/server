package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

/** 반의 차시마다 학생 한 명이 한 번만 제출한다. */
@Entity
@Table(name = "lesson_submissions")
class LessonSubmission private constructor(classroom: Classroom, lessonNumber: Int, userId: UUID, questionCount: Int) :
    BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesson_submissions_seq")
    @SequenceGenerator(name = "lesson_submissions_seq", sequenceName = "lesson_submissions_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    var classroom: Classroom = classroom
        protected set

    @Column(name = "lesson_number", nullable = false, updatable = false)
    var lessonNumber: Int = lessonNumber
        protected set

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Column(name = "correct_count", nullable = false)
    var correctCount: Int = 0
        protected set

    @Column(name = "question_count", nullable = false, updatable = false)
    var questionCount: Int = questionCount
        protected set

    @OneToMany(mappedBy = "submission", cascade = [CascadeType.ALL], orphanRemoval = true)
    val answers: MutableList<SubmissionAnswer> = mutableListOf()

    fun addAnswer(question: Question, optionId: Long?, text: String?) {
        val answer = SubmissionAnswer.create(this, question, optionId, text)
        answers.add(answer)
        if (answer.isCorrect) correctCount += 1
    }

    companion object {
        fun create(classroom: Classroom, lessonNumber: Int, userId: UUID, questionCount: Int): LessonSubmission =
            LessonSubmission(classroom, lessonNumber, userId, questionCount)
    }
}
