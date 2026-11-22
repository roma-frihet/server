package com.romafrihet.server.lesson

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime

interface LessonScheduleRepository : JpaRepository<LessonSchedule, Long> {
    fun findByClassroomIdAndLessonNumber(classroomId: Long, lessonNumber: Int): LessonSchedule?

    fun findAllByClassroomIdIn(classroomIds: Collection<Long>): List<LessonSchedule>

    /**
     * 시각이 지난 예약이다. 여러 인스턴스가 같은 예약을 동시에 적용하지 않도록 행을 잠그고,
     * 다른 쪽이 잡고 있는 행은 기다리지 않고 건너뛴다(-2는 Hibernate의 SKIP LOCKED).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @EntityGraph(attributePaths = ["classroom", "classroom.course"])
    @Query(
        """
        select s from LessonSchedule s
        where s.classroom.id in :classroomIds
          and (s.openAt <= :now or s.finishAt <= :now)
        """,
    )
    fun findDue(classroomIds: Collection<Long>, now: LocalDateTime): List<LessonSchedule>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LessonSchedule s where s.classroom.id in :classroomIds")
    fun deleteAllByClassroomIdIn(classroomIds: Collection<Long>)

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LessonSchedule s where s.classroom.course.id = :courseId and s.lessonNumber = :lessonNumber")
    fun deleteAllByCourseIdAndLessonNumber(courseId: Long, lessonNumber: Int)
}
