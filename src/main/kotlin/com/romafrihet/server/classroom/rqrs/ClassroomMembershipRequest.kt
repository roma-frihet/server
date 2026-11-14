package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

class ClassroomMembershipRequest private constructor() {
    @Schema(name = "ClassroomMembershipRequestQuery")
    data class Query(val status: MembershipStatus? = null) {
        fun toCommand(userId: UUID, classroomId: Long) =
            ClassroomMembershipCommand.Query(userId = userId, classroomId = classroomId, status = status)
    }
}
