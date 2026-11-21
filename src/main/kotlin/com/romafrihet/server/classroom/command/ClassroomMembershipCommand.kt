package com.romafrihet.server.classroom.command

import java.util.UUID

class ClassroomMembershipCommand private constructor() {
    data class Join(val userId: UUID, val joinCode: String)

    data class Create(val userId: UUID, val studentName: String, val studentNumber: String)

    data class Query(val userId: UUID, val classroomId: Long)

    data class RemoveStudent(val userId: UUID, val classroomId: Long, val membershipId: Long)
}
