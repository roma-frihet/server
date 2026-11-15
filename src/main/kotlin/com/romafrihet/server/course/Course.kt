package com.romafrihet.server.course

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.school.School
import com.romafrihet.server.subject.Subject
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
import java.util.UUID

/** 교사 + 학년 + 과목 단위다. 차시와 문제는 수업에 속하고, 같은 수업의 반들이 공유한다. */
@Entity
@Table(name = "courses")
class Course private constructor(userId: UUID, school: School, grade: Int, subject: Subject) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "courses_seq")
    @SequenceGenerator(name = "courses_seq", sequenceName = "courses_seq", allocationSize = 50)
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false, updatable = false)
    var school: School = school
        protected set

    @Column(nullable = false, updatable = false)
    var grade: Int = grade
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, updatable = false)
    var subject: Subject = subject
        protected set

    companion object {
        fun create(school: School, subject: Subject, command: CourseCommand.Create): Course =
            Course(userId = command.userId, school = school, grade = command.grade, subject = subject)
    }
}
