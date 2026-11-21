package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.projection.ClassroomStudentCountProjection
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ClassroomMembershipRepository : JpaRepository<ClassroomMembership, Long> {
    fun findByIdAndClassroomId(id: Long, classroomId: Long): ClassroomMembership?

    fun findAllByClassroomIdOrderByIdAsc(classroomId: Long): List<ClassroomMembership>

    @EntityGraph(attributePaths = ["classroom.course"])
    fun findAllByUserIdOrderByIdAsc(userId: UUID): List<ClassroomMembership>

    /** 한 수업에는 한 번만 연결한다. 반이 달라도 같은 수업이면 다시 넣을 수 없다. */
    fun existsByClassroomCourseIdAndUserId(courseId: Long, userId: UUID): Boolean

    fun existsByClassroomIdAndUserId(classroomId: Long, userId: UUID): Boolean

    fun existsByClassroomId(classroomId: Long): Boolean

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<ClassroomMembership>

    /** 반을 지우면 연결도 끊는다. 학생 계정과 가입 정보는 남는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ClassroomMembership m where m.classroom.id in :classroomIds")
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Query(
        """
        select new com.romafrihet.server.classroom.projection.ClassroomStudentCountProjection(m.classroom.id, count(m))
        from ClassroomMembership m
        where m.classroom.id in :classroomIds
        group by m.classroom.id
        """,
    )
    fun findStudentCounts(classroomIds: Collection<Long>): List<ClassroomStudentCountProjection>
}
