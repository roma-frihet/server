package com.romafrihet.server.student

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.student.command.StudentPasswordCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/**
 * 비밀번호는 Supabase Auth가 갖고 있다. 서버는 바꿔 달라고 요청하고 플래그만 관리한다.
 * Supabase 호출은 외부 API라 트랜잭션 밖에서 한다.
 */
@Service
class StudentPasswordService(
    private val studentProfileRepository: StudentProfileRepository,
    private val supabaseAuthClient: SupabaseAuthClient,
    private val temporaryPasswordGenerator: TemporaryPasswordGenerator,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    /**
     * 임시 비밀번호를 만들어 한 번만 돌려준다. 부르는 쪽에서 교사 소유를 먼저 확인한다.
     * Supabase가 비밀번호를 바꾸면서 그 학생의 refresh 토큰도 무효화한다.
     */
    fun reset(studentId: UUID): String {
        val temporary = temporaryPasswordGenerator.generate()
        supabaseAuthClient.updatePassword(studentId, temporary)
        inTransaction { profileOf(studentId).markPasswordReset() }
        return temporary
    }

    /** 학생이 새 비밀번호를 정한다. 임시 비밀번호 상태를 푼다. */
    fun change(command: StudentPasswordCommand.Change) {
        val userId = inTransaction { profileOf(command.userId).userId }
        supabaseAuthClient.updatePassword(userId, command.password)
        inTransaction { profileOf(userId).markPasswordChanged() }
    }

    private fun profileOf(userId: UUID): StudentProfile = studentProfileRepository.findByUserId(userId)
        ?: throw BusinessException(ErrorCode.STUDENT_PROFILE_REQUIRED)

    private fun <T> inTransaction(block: () -> T): T = requireNotNull(transactionTemplate.execute { block() })
}
