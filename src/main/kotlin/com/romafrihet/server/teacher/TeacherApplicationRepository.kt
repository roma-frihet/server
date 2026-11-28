package com.romafrihet.server.teacher

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TeacherApplicationRepository : JpaRepository<TeacherApplication, Long> {
    @EntityGraph(attributePaths = ["school"])
    fun findByUserId(userId: UUID): TeacherApplication?

    fun findAllByUserIdIn(userIds: Collection<UUID>): List<TeacherApplication>

    @EntityGraph(attributePaths = ["school"])
    fun findAllByStatusIn(statuses: Collection<TeacherApplicationStatus>, pageable: Pageable): Page<TeacherApplication>
}

fun TeacherApplicationRepository.getOrThrow(id: Long): TeacherApplication =
    findById(id).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
