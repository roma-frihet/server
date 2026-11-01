package com.romafrihet.server.school

import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

/** 학교는 마이그레이션으로만 등록한다. */
@Entity
@Table(name = "schools")
class School private constructor(name: String) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "schools_seq")
    @SequenceGenerator(name = "schools_seq", sequenceName = "schools_seq", allocationSize = 50)
    val id: Long? = null

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set
}
