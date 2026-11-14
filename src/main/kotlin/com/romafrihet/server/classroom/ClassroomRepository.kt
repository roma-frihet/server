package com.romafrihet.server.classroom

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClassroomRepository : JpaRepository<Classroom, Long> {
    fun existsByCourseIdAndName(courseId: Long, name: String): Boolean

    fun findByIdAndCourseUserId(id: Long, userId: UUID): Classroom?

    fun findByJoinCode(joinCode: String): Classroom?

    fun findAllByCourseUserIdOrderByNameAsc(userId: UUID): List<Classroom>

    fun findAllByCourseIdOrderByNameAsc(courseId: Long): List<Classroom>

    fun existsByCourseId(courseId: Long): Boolean
}

/** 다른 교사의 반은 존재 자체를 알리지 않도록 404로 응답한다. */
fun ClassroomRepository.getOwned(id: Long, userId: UUID): Classroom =
    findByIdAndCourseUserId(id, userId) ?: throw BusinessException(ErrorCode.NOT_FOUND)
