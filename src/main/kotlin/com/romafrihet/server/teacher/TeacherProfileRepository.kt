package com.romafrihet.server.teacher

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeacherProfileRepository : JpaRepository<TeacherProfile, Long> {
    @EntityGraph(attributePaths = ["school"])
    fun findByUserId(userId: UUID): TeacherProfile?
}
