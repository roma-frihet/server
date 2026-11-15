package com.romafrihet.server.teacher

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.student.StudentPasswordService
import com.romafrihet.server.teacher.command.TeacherCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 교사는 자기 반 학생의 비밀번호만 초기화한다. 실제 변경은 학생 기능에 맡긴다.
 *
 * 클래스에 @Transactional을 두지 않는다. 두면 Supabase 호출이 그 트랜잭션 안으로 들어가고,
 * readOnly 때문에 플래그 저장도 날아간다.
 */
@Service
class StudentPasswordResetService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val studentPasswordService: StudentPasswordService,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun reset(command: TeacherCommand.ResetStudentPassword): String {
        val studentId = ownedStudentId(command)
        return studentPasswordService.reset(studentId)
    }

    private fun ownedStudentId(command: TeacherCommand.ResetStudentPassword): UUID = requireNotNull(
        transactionTemplate.execute {
            classroomRepository.getOwned(command.classroomId, command.userId)
            classroomMembershipRepository
                .findByIdAndClassroomId(command.membershipId, command.classroomId)
                ?.userId
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        },
    )
}
