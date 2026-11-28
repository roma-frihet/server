package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.util.UUID

/** 관리자가 정지한 계정이다. 로그인을 막는 것은 Supabase ban이고, 이 행은 목록에 정지 여부를 보이려고 둔다. */
@Entity
@Table(name = "account_suspensions")
class AccountSuspension private constructor(userId: UUID, reason: String?, suspendedBy: UUID) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_suspensions_seq")
    @SequenceGenerator(name = "account_suspensions_seq", sequenceName = "account_suspensions_seq", allocationSize = 50)
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Column(length = 200)
    var reason: String? = reason
        protected set

    @Column(name = "suspended_by", nullable = false)
    var suspendedBy: UUID = suspendedBy
        protected set

    fun update(command: AdminCommand.Suspend) {
        reason = command.reason
        suspendedBy = command.adminId
    }

    companion object {
        fun create(command: AdminCommand.Suspend): AccountSuspension =
            AccountSuspension(userId = command.userId, reason = command.reason, suspendedBy = command.adminId)
    }
}
