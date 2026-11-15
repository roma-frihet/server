package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.classroom.dto.ClassroomMembershipDto
import com.romafrihet.server.classroom.event.ClassroomMembershipRemovedEvent
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.student.StudentProfileRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ClassroomMembershipService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    /** 교사 승인은 없다. 유효한 코드를 넣으면 바로 연결된다. */
    @Transactional
    fun join(command: ClassroomMembershipCommand.Join): ClassroomMembershipDto {
        val profile =
            studentProfileRepository.findByUserId(command.userId)
                ?: throw BusinessException(ErrorCode.STUDENT_PROFILE_REQUIRED)
        val classroom =
            classroomRepository.findByJoinCode(command.joinCode)
                ?: throw BusinessException(ErrorCode.JOIN_CODE_INVALID)
        assertCheck(classroom.isJoinCodeActive(LocalDateTime.now(clock))) { ErrorCode.JOIN_CODE_EXPIRED }

        val courseId = requireNotNull(classroom.course.id)
        assertCheck(!classroomMembershipRepository.existsByClassroomCourseIdAndUserId(courseId, command.userId)) {
            ErrorCode.ALREADY_ENROLLED
        }
        val request =
            ClassroomMembershipCommand.Create(
                userId = command.userId,
                studentName = profile.name,
                studentNumber = profile.studentNumber(),
            )
        return toDto(classroomMembershipRepository.save(ClassroomMembership.create(classroom, request)))
    }

    fun findAllForTeacher(command: ClassroomMembershipCommand.Query): List<ClassroomMembershipDto> {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        return classroomMembershipRepository
            .findAllByClassroomIdOrderByIdAsc(requireNotNull(classroom.id))
            .map(::toDto)
    }

    fun findAllForStudent(userId: UUID): List<ClassroomMembershipDto> =
        classroomMembershipRepository.findAllByUserIdOrderByIdAsc(userId).map(::toDto)

    /** 코드를 잘못 넣어 엉뚱한 반에 들어온 학생을 되돌린다. 그 반에서 푼 기록도 함께 지운다. */
    @Transactional
    fun removeStudent(command: ClassroomMembershipCommand.RemoveStudent) {
        classroomRepository.getOwned(command.classroomId, command.userId)
        val membership =
            classroomMembershipRepository.findByIdAndClassroomId(command.membershipId, command.classroomId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        classroomMembershipRepository.delete(membership)
        eventPublisher.publishEvent(
            ClassroomMembershipRemovedEvent(classroomId = command.classroomId, userId = membership.userId),
        )
    }

    private fun toDto(membership: ClassroomMembership) = ClassroomMembershipDto(
        id = requireNotNull(membership.id),
        classroomId = requireNotNull(membership.classroom.id),
        classNo = membership.classroom.classNo,
        grade = membership.classroom.course.grade,
        subjectName = membership.classroom.course.subject.name,
        studentName = membership.studentName,
        studentNumber = membership.studentNumber,
        joinedAt = membership.createdAt,
    )
}
