package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.course.Course
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
@Table(name = "classrooms")
class Classroom private constructor(course: Course, name: String) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "classrooms_seq")
    @SequenceGenerator(name = "classrooms_seq", sequenceName = "classrooms_seq", allocationSize = 50)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    var course: Course = course
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "join_code", length = 8)
    var joinCode: String? = null
        protected set

    @Column(name = "join_code_expires_at")
    var joinCodeExpiresAt: LocalDateTime? = null
        protected set

    fun issueJoinCode(code: String, expiresAt: LocalDateTime) {
        joinCode = code
        joinCodeExpiresAt = expiresAt
    }

    fun closeJoinCode() {
        joinCode = null
        joinCodeExpiresAt = null
    }

    companion object {
        fun create(course: Course, command: ClassroomCommand.Create): Classroom =
            Classroom(course = course, name = command.name)
    }
}
