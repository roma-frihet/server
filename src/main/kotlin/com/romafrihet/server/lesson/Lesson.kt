package com.romafrihet.server.lesson

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.course.Course
import com.romafrihet.server.lesson.command.LessonCommand
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
@Table(name = "lessons")
class Lesson private constructor(course: Course, lessonNumber: Int, title: String) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lessons_seq")
    @SequenceGenerator(name = "lessons_seq", sequenceName = "lessons_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    var course: Course = course
        protected set

    @Column(name = "lesson_number", nullable = false, updatable = false)
    var lessonNumber: Int = lessonNumber
        protected set

    @Column(nullable = false, length = 100)
    var title: String = title
        protected set

    fun rename(command: LessonCommand.Rename) {
        title = command.title
    }

    companion object {
        fun create(course: Course, lessonNumber: Int, command: LessonCommand.Create): Lesson =
            Lesson(course = course, lessonNumber = lessonNumber, title = command.title)
    }
}
