package com.romafrihet.server.teacher

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.school.School
import com.romafrihet.server.teacher.command.TeacherCommand
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
@Table(name = "teacher_profiles")
class TeacherProfile private constructor(userId: UUID, school: School, name: String) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "teacher_profiles_seq")
    @SequenceGenerator(name = "teacher_profiles_seq", sequenceName = "teacher_profiles_seq", allocationSize = 50)
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    var school: School = school
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    /** 학교를 옮겨도 기존 수업의 학교는 바꾸지 않아 반과 기록이 그대로 남는다. */
    fun update(school: School, command: TeacherCommand.SaveProfile) {
        this.school = school
        name = command.name
    }

    companion object {
        fun create(school: School, command: TeacherCommand.SaveProfile): TeacherProfile =
            TeacherProfile(userId = command.userId, school = school, name = command.name)
    }
}
