package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.projection.ClassroomStudentCountProjection
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ClassroomMembershipRepository : JpaRepository<ClassroomMembership, Long> {
    fun findByClassroomIdAndUserId(classroomId: Long, userId: UUID): ClassroomMembership?

    fun findByIdAndClassroomId(id: Long, classroomId: Long): ClassroomMembership?

    fun findAllByClassroomIdOrderByIdAsc(classroomId: Long): List<ClassroomMembership>

    fun findAllByClassroomIdAndStatusOrderByIdAsc(
        classroomId: Long,
        status: MembershipStatus,
    ): List<ClassroomMembership>

    @EntityGraph(attributePaths = ["classroom.course"])
    fun findAllByUserIdOrderByIdAsc(userId: UUID): List<ClassroomMembership>

    fun existsByClassroomIdAndUserIdAndStatus(classroomId: Long, userId: UUID, status: MembershipStatus): Boolean

    fun existsByClassroomId(classroomId: Long): Boolean

    fun findAllByClassroomIdInAndStatus(
        classroomIds: Collection<Long>,
        status: MembershipStatus,
    ): List<ClassroomMembership>

    @Query(
        """
        select new com.romafrihet.server.classroom.projection.ClassroomStudentCountProjection(m.classroom.id, count(m))
        from ClassroomMembership m
        where m.classroom.id in :classroomIds and m.status = :status
        group by m.classroom.id
        """,
    )
    fun findStudentCounts(
        classroomIds: Collection<Long>,
        status: MembershipStatus,
    ): List<ClassroomStudentCountProjection>
}
