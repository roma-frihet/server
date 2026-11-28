package com.romafrihet.server.teacher

import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.school.School
import com.romafrihet.server.teacher.command.TeacherApplicationCommand
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * 교사 가입 신청이다. 사용자당 한 행이고, 거절된 뒤 다시 신청하면 같은 행을 대기로 되돌린다.
 * 실제 권한은 Supabase app_metadata.role이 갖고, 이 행은 그 결정을 내린 기록이다.
 */
@Entity
@Table(name = "teacher_applications")
class TeacherApplication private constructor(userId: UUID, email: String, school: School, name: String) :
    BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "teacher_applications_seq")
    @SequenceGenerator(
        name = "teacher_applications_seq",
        sequenceName = "teacher_applications_seq",
        allocationSize = 50,
    )
    val id: Long? = null

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Column(nullable = false)
    var email: String = email
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    var school: School = school
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TeacherApplicationStatus = TeacherApplicationStatus.PENDING
        protected set

    @Column(name = "reject_reason", length = 200)
    var rejectReason: String? = null
        protected set

    /** 이메일 인증을 붙이면 채운다. 이메일을 바꿔 다시 신청하면 비운다. */
    @Column(name = "email_verified_at")
    var emailVerifiedAt: LocalDateTime? = null
        protected set

    @Column(name = "reviewed_by")
    var reviewedBy: UUID? = null
        protected set

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null
        protected set

    /** 대기 중에 고치거나 거절된 뒤 다시 신청한다. 승인되었거나 권한을 거둔 계정은 관리자만 되돌린다. */
    fun reapply(school: School, command: TeacherApplicationCommand.Apply) {
        assertCheck(status == TeacherApplicationStatus.PENDING || status == TeacherApplicationStatus.REJECTED) {
            ErrorCode.TEACHER_APPLICATION_CLOSED
        }
        this.school = school
        name = command.name
        val newEmail = requireNotNull(command.email)
        if (!newEmail.equals(email, ignoreCase = true)) emailVerifiedAt = null
        email = newEmail
        status = TeacherApplicationStatus.PENDING
        rejectReason = null
        reviewedBy = null
        reviewedAt = null
    }

    fun markEmailVerified(now: LocalDateTime) {
        emailVerifiedAt = now
    }

    /** 거절했거나 권한을 거둔 계정도 관리자가 마음을 바꾸면 승인할 수 있다. */
    fun approve(reviewerId: UUID, now: LocalDateTime) {
        assertCheck(status != TeacherApplicationStatus.APPROVED) { ErrorCode.TEACHER_APPLICATION_NOT_PENDING }
        review(TeacherApplicationStatus.APPROVED, reviewerId, now)
        rejectReason = null
    }

    fun reject(reviewerId: UUID, reason: String?, now: LocalDateTime) {
        assertCheck(status == TeacherApplicationStatus.PENDING) { ErrorCode.TEACHER_APPLICATION_NOT_PENDING }
        review(TeacherApplicationStatus.REJECTED, reviewerId, now)
        rejectReason = reason
    }

    fun revoke(reviewerId: UUID, now: LocalDateTime) {
        assertCheck(status == TeacherApplicationStatus.APPROVED) { ErrorCode.TEACHER_APPLICATION_NOT_PENDING }
        review(TeacherApplicationStatus.REVOKED, reviewerId, now)
    }

    private fun review(status: TeacherApplicationStatus, reviewerId: UUID, now: LocalDateTime) {
        this.status = status
        reviewedBy = reviewerId
        reviewedAt = now
    }

    companion object {
        fun create(school: School, command: TeacherApplicationCommand.Apply): TeacherApplication = TeacherApplication(
            userId = command.userId,
            email = requireNotNull(command.email),
            school = school,
            name = command.name,
        )

        /**
         * 신청 기능이 생기기 전에 SQL로 역할을 받은 교사는 신청 행이 없다. 권한을 거둘 때 기록을 남기려고
         * 승인된 신청을 만들어 둔다.
         */
        fun approvedLegacy(profile: TeacherProfile, email: String, now: LocalDateTime): TeacherApplication =
            TeacherApplication(userId = profile.userId, email = email, school = profile.school, name = profile.name)
                .apply { review(TeacherApplicationStatus.APPROVED, profile.userId, now) }
    }
}
