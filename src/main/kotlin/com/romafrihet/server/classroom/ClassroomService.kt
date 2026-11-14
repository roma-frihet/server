package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.dto.ClassroomDto
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ClassroomService(
    private val classroomRepository: ClassroomRepository,
    private val courseRepository: CourseRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val joinCodeGenerator: JoinCodeGenerator,
) {
    @Transactional
    fun create(command: ClassroomCommand.Create): ClassroomDto {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        assertCheck(!classroomRepository.existsByCourseIdAndName(command.courseId, command.name)) {
            ErrorCode.CLASSROOM_NAME_DUPLICATED
        }
        return toDto(classroomRepository.save(Classroom.create(course, command)), studentCount = 0)
    }

    fun findAll(userId: UUID): List<ClassroomDto> {
        val classrooms = classroomRepository.findAllByCourseUserIdOrderByNameAsc(userId)
        val counts = studentCounts(classrooms.map { requireNotNull(it.id) })
        return classrooms.map { toDto(it, counts[it.id] ?: 0) }
    }

    /** 다시 발급하면 이전 코드는 덮어써져 바로 쓸 수 없게 된다. */
    @Transactional
    fun issueJoinCode(command: ClassroomCommand.IssueJoinCode): ClassroomDto {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        classroom.issueJoinCode(
            code = joinCodeGenerator.generate(),
            expiresAt = LocalDateTime.now().plusMinutes(JOIN_CODE_VALID_MINUTES),
        )
        return toDto(classroom, studentCounts(listOf(command.classroomId))[command.classroomId] ?: 0)
    }

    /** 코드를 끄면 이미 합류한 학생은 그대로 두고, 새 합류만 막는다. */
    @Transactional
    fun closeJoinCode(command: ClassroomCommand.IssueJoinCode) {
        classroomRepository.getOwned(command.classroomId, command.userId).closeJoinCode()
    }

    private fun studentCounts(classroomIds: List<Long>): Map<Long, Long> = classroomMembershipRepository
        .findStudentCounts(classroomIds, MembershipStatus.APPROVED)
        .associate { it.classroomId to it.studentCount }

    private fun toDto(classroom: Classroom, studentCount: Long) = ClassroomDto(
        id = requireNotNull(classroom.id),
        courseId = requireNotNull(classroom.course.id),
        name = classroom.name,
        joinCode = classroom.joinCode,
        joinCodeExpiresAt = classroom.joinCodeExpiresAt,
        studentCount = studentCount,
    )

    private companion object {
        const val JOIN_CODE_VALID_MINUTES = 3L
    }
}
