package com.romafrihet.server.classroom.command

import com.romafrihet.server.classroom.MembershipStatus
import java.util.UUID

class ClassroomMembershipCommand private constructor() {
    data class Join(val userId: UUID, val joinCode: String)

    data class Create(val userId: UUID, val studentName: String, val studentNumber: String)

    data class Query(val userId: UUID, val classroomId: Long, val status: MembershipStatus?)

    data class ApproveAll(val userId: UUID, val classroomId: Long)

    data class Decide(val userId: UUID, val classroomId: Long, val membershipId: Long)
}
