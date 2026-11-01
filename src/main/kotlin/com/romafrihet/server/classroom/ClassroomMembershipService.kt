package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.classroom.dto.ClassroomMembershipDto
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.student.StudentProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ClassroomMembershipService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val studentProfileRepository: StudentProfileRepository,
) {
    @Transactional
    fun join(command: ClassroomMembershipCommand.Join): ClassroomMembershipDto {
        val profile =
            studentProfileRepository.findByUserId(command.userId)
                ?: throw BusinessException(ErrorCode.STUDENT_PROFILE_REQUIRED)
        val classroom =
            classroomRepository.findByJoinCode(command.joinCode)
                ?: throw BusinessException(ErrorCode.JOIN_CODE_INVALID)
        assertCheck(classroom.isJoinCodeActive(LocalDateTime.now())) { ErrorCode.JOIN_CODE_EXPIRED }

        val classroomId = requireNotNull(classroom.id)
        val request =
            ClassroomMembershipCommand.Create(
                userId = command.userId,
                studentName = profile.name,
                studentNumber = profile.studentNumber(),
            )
        val existing = classroomMembershipRepository.findByClassroomIdAndUserId(classroomId, command.userId)
        val membership =
            existing?.apply { reapply(request) }
                ?: classroomMembershipRepository.save(ClassroomMembership.create(classroom, request))
        return toDto(membership)
    }

    fun findAllForTeacher(command: ClassroomMembershipCommand.Query): List<ClassroomMembershipDto> {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val classroomId = requireNotNull(classroom.id)
        val memberships =
            command.status?.let {
                classroomMembershipRepository.findAllByClassroomIdAndStatusOrderByIdAsc(classroomId, it)
            } ?: classroomMembershipRepository.findAllByClassroomIdOrderByIdAsc(classroomId)
        return memberships.map(::toDto)
    }

    fun findAllForStudent(userId: UUID): List<ClassroomMembershipDto> =
        classroomMembershipRepository.findAllByUserIdOrderByIdAsc(userId).map(::toDto)

    @Transactional
    fun approve(command: ClassroomMembershipCommand.Decide): ClassroomMembershipDto {
        val membership = getOwnedMembership(command)
        membership.approve()
        return toDto(membership)
    }

    @Transactional
    fun reject(command: ClassroomMembershipCommand.Decide): ClassroomMembershipDto {
        val membership = getOwnedMembership(command)
        membership.reject()
        return toDto(membership)
    }

    @Transactional
    fun approveAll(command: ClassroomMembershipCommand.ApproveAll): List<ClassroomMembershipDto> {
        classroomRepository.getOwned(command.classroomId, command.userId)
        return classroomMembershipRepository
            .findAllByClassroomIdAndStatusOrderByIdAsc(command.classroomId, MembershipStatus.PENDING)
            .onEach(ClassroomMembership::approve)
            .map(::toDto)
    }

    private fun getOwnedMembership(command: ClassroomMembershipCommand.Decide): ClassroomMembership {
        classroomRepository.getOwned(command.classroomId, command.userId)
        return classroomMembershipRepository.findByIdAndClassroomId(command.membershipId, command.classroomId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND)
    }

    private fun toDto(membership: ClassroomMembership) = ClassroomMembershipDto(
        id = requireNotNull(membership.id),
        classroomId = requireNotNull(membership.classroom.id),
        classroomName = membership.classroom.name,
        grade = membership.classroom.course.grade,
        subject = membership.classroom.course.subject,
        studentName = membership.studentName,
        studentNumber = membership.studentNumber,
        status = membership.status,
        requestedAt = membership.updatedAt,
    )
}
