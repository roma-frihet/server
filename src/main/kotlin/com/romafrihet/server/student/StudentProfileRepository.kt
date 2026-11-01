package com.romafrihet.server.student

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StudentProfileRepository : JpaRepository<StudentProfile, Long> {
    @EntityGraph(attributePaths = ["school"])
    fun findByUserId(userId: UUID): StudentProfile?

    fun existsByUserId(userId: UUID): Boolean

    fun existsBySchoolIdAndGradeAndClassNumberAndSeatNumber(
        schoolId: Long,
        grade: Int,
        classNumber: Int,
        seatNumber: Int,
    ): Boolean
}
