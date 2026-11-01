package com.romafrihet.server.student

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.school.School
import com.romafrihet.server.student.command.StudentProfileCommand
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

@Entity
@Table(name = "student_profiles")
class StudentProfile private constructor(
    userId: UUID,
    school: School,
    grade: Int,
    classNumber: Int,
    seatNumber: Int,
    name: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "student_profiles_seq")
    @SequenceGenerator(name = "student_profiles_seq", sequenceName = "student_profiles_seq", allocationSize = 50)
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false, updatable = false)
    var school: School = school
        protected set

    @Column(nullable = false)
    var grade: Int = grade
        protected set

    @Column(name = "class_number", nullable = false)
    var classNumber: Int = classNumber
        protected set

    @Column(name = "seat_number", nullable = false)
    var seatNumber: Int = seatNumber
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    /** 2학년 1반 1번은 20101이다. */
    fun studentNumber(): String = "%d%02d%02d".format(grade, classNumber, seatNumber)

    companion object {
        fun create(school: School, command: StudentProfileCommand.Create): StudentProfile = StudentProfile(
            userId = command.userId,
            school = school,
            grade = command.grade,
            classNumber = command.classNumber,
            seatNumber = command.seatNumber,
            name = command.name,
        )
    }
}
