package com.romafrihet.server.classroom.rqrs

import com.romafrihet.server.classroom.dto.ClassroomMembershipDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class ClassroomMembershipResponse private constructor() {
    @Schema(name = "ClassroomMembershipResponseResult")
    data class Result(
        val id: Long,
        val classroomId: Long,
        val classNo: Int,
        val grade: Int,
        val subjectName: String,
        val studentName: String,
        val studentNumber: String,
        val joinedAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: ClassroomMembershipDto) = Result(
                id = dto.id,
                classroomId = dto.classroomId,
                classNo = dto.classNo,
                grade = dto.grade,
                subjectName = dto.subjectName,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                joinedAt = dto.joinedAt,
            )
        }
    }
}
