package com.romafrihet.server.classroom

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ClassroomRepository : JpaRepository<Classroom, Long> {
    fun existsByCourseIdAndClassNo(courseId: Long, classNo: Int): Boolean

    fun findByIdAndCourseUserId(id: Long, userId: UUID): Classroom?

    fun findByJoinCode(joinCode: String): Classroom?

    /** 반 목록은 수업 정보까지 함께 내리므로 course와 subject를 같이 가져와 N+1을 막는다. */
    @EntityGraph(attributePaths = ["course", "course.subject"])
    fun findAllByCourseUserIdOrderByCourseIdAscClassNoAsc(userId: UUID): List<Classroom>

    fun findAllByCourseIdOrderByClassNoAsc(courseId: Long): List<Classroom>

    fun findAllByCourseIdAndIdInOrderByClassNoAsc(courseId: Long, ids: Collection<Long>): List<Classroom>

    fun existsByCourseId(courseId: Long): Boolean

    /**
     * 반의 차시를 여는 쪽(교사의 직접 열기, 예약 적용)끼리 줄을 세우려고 반 행을 잠근다.
     * 여러 반을 잠글 때 순서가 엇갈려 교착되지 않도록 id 순으로 잠근다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classroom c where c.id in :ids order by c.id")
    fun findAllForUpdate(ids: Collection<Long>): List<Classroom>
}

/** 다른 교사의 반은 존재 자체를 알리지 않도록 404로 응답한다. */
fun ClassroomRepository.getOwned(id: Long, userId: UUID): Classroom =
    findByIdAndCourseUserId(id, userId) ?: throw BusinessException(ErrorCode.NOT_FOUND)
