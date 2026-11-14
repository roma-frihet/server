package com.romafrihet.server.course

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CourseRepository : JpaRepository<Course, Long> {
    fun findByIdAndUserId(id: Long, userId: UUID): Course?

    fun existsByUserIdAndGradeAndSubject(userId: UUID, grade: Int, subject: String): Boolean

    @EntityGraph(attributePaths = ["school"])
    fun findAllByUserIdOrderByGradeAscSubjectAsc(userId: UUID): List<Course>
}

/** 다른 교사의 수업은 존재 자체를 알리지 않도록 404로 응답한다. */
fun CourseRepository.getOwned(id: Long, userId: UUID): Course =
    findByIdAndUserId(id, userId) ?: throw BusinessException(ErrorCode.NOT_FOUND)
