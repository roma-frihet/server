package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
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
import java.time.LocalDateTime

@Entity
@Table(name = "lesson_openings")
class LessonOpening private constructor(classroom: Classroom, lessonNumber: Int) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesson_openings_seq")
    @SequenceGenerator(name = "lesson_openings_seq", sequenceName = "lesson_openings_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    var classroom: Classroom = classroom
        protected set

    @Column(name = "lesson_number", nullable = false, updatable = false)
    var lessonNumber: Int = lessonNumber
        protected set

    @Column(name = "opened_at", nullable = false)
    var openedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null
        protected set

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null
        protected set

    val isOpen: Boolean
        get() = closedAt == null

    val status: LessonStatus
        get() =
            when {
                finishedAt != null -> LessonStatus.FINISHED
                closedAt != null -> LessonStatus.CLOSED
                else -> LessonStatus.OPEN
            }

    fun reopen() {
        assertCheck(status == LessonStatus.CLOSED) { ErrorCode.LESSON_FINISHED }
        openedAt = LocalDateTime.now()
        closedAt = null
    }

    fun close() {
        closedAt = LocalDateTime.now()
    }

    fun finish() {
        assertCheck(status != LessonStatus.FINISHED) { ErrorCode.LESSON_FINISHED }
        val now = LocalDateTime.now()
        closedAt = closedAt ?: now
        finishedAt = now
    }

    companion object {
        fun create(classroom: Classroom, lessonNumber: Int): LessonOpening = LessonOpening(classroom, lessonNumber)
    }
}
