package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.security.SupabaseAuthClient
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 계정을 정지하면 Supabase가 로그인과 토큰 갱신을 막는다. 이미 받은 access 토큰은 만료(1시간)까지 남는다.
 * Supabase 호출을 먼저 하고 기록을 남긴다. 기록이 실패해도 다시 부르면 같은 결과가 된다.
 */
@Service
class AccountSuspensionService(
    private val accountSuspensionRepository: AccountSuspensionRepository,
    private val supabaseAuthClient: SupabaseAuthClient,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun suspend(command: AdminCommand.Suspend) {
        assertCheck(command.userId != command.adminId) { ErrorCode.ADMIN_SELF_ACTION }
        supabaseAuthClient.updateBan(command.userId, banned = true)
        transactionTemplate.executeWithoutResult {
            accountSuspensionRepository.findByUserId(command.userId)?.update(command)
                ?: accountSuspensionRepository.save(AccountSuspension.create(command))
        }
    }

    fun unsuspend(command: AdminCommand.Unsuspend) {
        assertCheck(command.userId != command.adminId) { ErrorCode.ADMIN_SELF_ACTION }
        supabaseAuthClient.updateBan(command.userId, banned = false)
        transactionTemplate.executeWithoutResult {
            accountSuspensionRepository.findByUserId(command.userId)?.let(accountSuspensionRepository::delete)
        }
    }
}
