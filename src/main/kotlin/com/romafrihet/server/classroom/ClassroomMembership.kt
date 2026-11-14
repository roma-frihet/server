package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.entity.BaseTimeEntity
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
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
import java.util.UUID

@Entity
@Table(name = "classroom_memberships")
class ClassroomMembership private constructor(
    classroom: Classroom,
    userId: UUID,
    studentName: String,
    studentNumber: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "classroom_memberships_seq")
    @SequenceGenerator(
        name = "classroom_memberships_seq",
        sequenceName = "classroom_memberships_seq",
        allocationSize = 50,
    )
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false, updatable = false)
    var classroom: Classroom = classroom
        protected set

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = userId
        protected set

    @Column(name = "student_name", nullable = false, length = 50)
    var studentName: String = studentName
        protected set

    @Column(name = "student_number", nullable = false, length = 20)
    var studentNumber: String = studentNumber
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MembershipStatus = MembershipStatus.PENDING
        protected set

    /** 거절된 학생이 다시 요청하면 입력을 새로 받아 대기 상태로 되돌린다. */
    fun reapply(command: ClassroomMembershipCommand.Create) {
        assertCheck(status == MembershipStatus.REJECTED) { ErrorCode.MEMBERSHIP_ALREADY_REQUESTED }
        studentName = command.studentName
        studentNumber = command.studentNumber
        status = MembershipStatus.PENDING
    }

    fun approve() {
        assertCheck(status == MembershipStatus.PENDING) { ErrorCode.MEMBERSHIP_NOT_PENDING }
        status = MembershipStatus.APPROVED
    }

    fun reject() {
        assertCheck(status == MembershipStatus.PENDING) { ErrorCode.MEMBERSHIP_NOT_PENDING }
        status = MembershipStatus.REJECTED
    }

    companion object {
        fun create(classroom: Classroom, command: ClassroomMembershipCommand.Create): ClassroomMembership =
            ClassroomMembership(
                classroom = classroom,
                userId = command.userId,
                studentName = command.studentName,
                studentNumber = command.studentNumber,
            )
    }
}
