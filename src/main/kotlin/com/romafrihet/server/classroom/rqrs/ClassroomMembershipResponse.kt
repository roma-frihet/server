package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.classroom.dto.ClassroomMembershipDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class ClassroomMembershipResponse private constructor() {
    @Schema(name = "ClassroomMembershipResponseResult")
    data class Result(
        val id: Long,
        val classroomId: Long,
        val classroomName: String,
        val grade: Int,
        val subject: String,
        val studentName: String,
        val studentNumber: String,
        val status: MembershipStatus,
        val requestedAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: ClassroomMembershipDto) = Result(
                id = dto.id,
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                grade = dto.grade,
                subject = dto.subject,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                status = dto.status,
                requestedAt = dto.requestedAt,
            )
        }
    }
}
