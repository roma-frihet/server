package com.romafrihet.server.admin

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AccountSuspensionRepository : JpaRepository<AccountSuspension, Long> {
    fun findByUserId(userId: UUID): AccountSuspension?

    fun findAllByUserIdIn(userIds: Collection<UUID>): List<AccountSuspension>
}
