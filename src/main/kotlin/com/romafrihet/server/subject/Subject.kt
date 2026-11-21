package com.romafrihet.server.subject

import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

/** 교육과정 과목이다. 행은 마이그레이션으로만 넣고 앱에서 만들지 않는다. */
@Entity
@Table(name = "subjects")
class Subject : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subjects_seq")
    @SequenceGenerator(name = "subjects_seq", sequenceName = "subjects_seq", allocationSize = 50)
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "school_level", nullable = false, updatable = false, length = 10)
    var schoolLevel: SchoolLevel = SchoolLevel.MIDDLE
        protected set

    @Column(name = "subject_group", nullable = false, updatable = false, length = 30)
    var subjectGroup: String = ""
        protected set

    @Column(nullable = false, updatable = false, length = 50)
    var name: String = ""
        protected set

    @Column(name = "display_order", nullable = false, updatable = false)
    var displayOrder: Int = 0
        protected set
}
