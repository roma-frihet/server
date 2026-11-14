package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

class ClassroomMembershipRequest private constructor() {
    @Schema(name = "ClassroomMembershipRequestJoin")
    data class Join(
        @field:NotBlank
        @field:Size(min = 6, max = 6)
        val joinCode: String?,
    ) {
        fun toCommand(userId: UUID) = ClassroomMembershipCommand.Join(
            userId = userId,
            joinCode = requireNotNull(joinCode).trim().uppercase(),
        )
    }

    @Schema(name = "ClassroomMembershipRequestQuery")
    data class Query(val status: MembershipStatus? = null) {
        fun toCommand(userId: UUID, classroomId: Long) =
            ClassroomMembershipCommand.Query(userId = userId, classroomId = classroomId, status = status)
    }
}
