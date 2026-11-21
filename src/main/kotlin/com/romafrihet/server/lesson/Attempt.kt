package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
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
import java.time.LocalDateTime
import java.util.UUID

/** 한 학생이 한 반에서 한 차시를 푼 기록이다. 문제는 순서대로만 진행하고 되돌아갈 수 없다. */
@Entity
@Table(name = "attempts")
class Attempt private constructor(classroom: Classroom, lessonNumber: Int, userId: UUID) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attempts_seq")
    @SequenceGenerator(name = "attempts_seq", sequenceName = "attempts_seq", allocationSize = 50)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var phase: AttemptPhase = AttemptPhase.READY
        protected set

    @Column(name = "next_index", nullable = false)
    var nextIndex: Int = 0
        protected set

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
        protected set

    fun startQuestion() {
        phase = AttemptPhase.IN_QUESTION
    }

    /** 문제가 확정되면 다음 문제로 넘어가고, 마지막이었으면 응시를 끝낸다. */
    fun completeQuestion(questionCount: Int, now: LocalDateTime) {
        nextIndex += 1
        if (nextIndex >= questionCount) {
            phase = AttemptPhase.DONE
            completedAt = now
        } else {
            phase = AttemptPhase.READY
        }
    }

    companion object {
        fun create(classroom: Classroom, lessonNumber: Int, userId: UUID): Attempt =
            Attempt(classroom, lessonNumber, userId)
    }
}
