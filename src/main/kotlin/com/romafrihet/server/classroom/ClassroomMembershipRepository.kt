package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.projection.ClassroomStudentCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClassroomMembershipRepository : JpaRepository<ClassroomMembership, Long> {
    fun findByIdAndClassroomId(id: Long, classroomId: Long): ClassroomMembership?

    fun findAllByClassroomIdOrderByIdAsc(classroomId: Long): List<ClassroomMembership>

    fun findAllByClassroomIdAndStatusOrderByIdAsc(
        classroomId: Long,
        status: MembershipStatus,
    ): List<ClassroomMembership>

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
