package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.Classroom
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
import java.time.LocalDateTime

/** 반의 차시를 정한 시각에 열고 마감하는 예약이다. 적용한 쪽은 비우고, 둘 다 비면 지운다. */
@Entity
@Table(name = "lesson_schedules")
class LessonSchedule private constructor(
    classroom: Classroom,
    lessonNumber: Int,
    openAt: LocalDateTime?,
    finishAt: LocalDateTime?,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesson_schedules_seq")
    @SequenceGenerator(name = "lesson_schedules_seq", sequenceName = "lesson_schedules_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    var classroom: Classroom = classroom
        protected set

    @Column(name = "lesson_number", nullable = false, updatable = false)
    var lessonNumber: Int = lessonNumber
        protected set

    @Column(name = "open_at")
    var openAt: LocalDateTime? = openAt
        protected set

    @Column(name = "finish_at")
    var finishAt: LocalDateTime? = finishAt
        protected set

    val isEmpty: Boolean
        get() = openAt == null && finishAt == null

    /** 정렬 기준이다. 먼저 일어날 일부터 적용해야 마감한 뒤에 다음 차시를 열 수 있다. */
    val firstAt: LocalDateTime
        get() = requireNotNull(openAt ?: finishAt)

    fun reschedule(openAt: LocalDateTime?, finishAt: LocalDateTime?) {
        this.openAt = openAt
        this.finishAt = finishAt
    }

    fun clearOpen() {
        openAt = null
    }

    fun clearFinish() {
        finishAt = null
    }

    companion object {
        fun create(
            classroom: Classroom,
            lessonNumber: Int,
            openAt: LocalDateTime?,
            finishAt: LocalDateTime?,
        ): LessonSchedule = LessonSchedule(classroom, lessonNumber, openAt, finishAt)
    }
}
