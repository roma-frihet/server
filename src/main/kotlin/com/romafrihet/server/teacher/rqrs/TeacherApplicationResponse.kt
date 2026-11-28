package com.romafrihet.server.teacher.rqrs

import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.dto.TeacherApplicationDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class TeacherApplicationResponse private constructor() {
    @Schema(name = "TeacherApplicationResponseApplication")
    data class Application(
        val id: Long,
        val email: String,
        val schoolId: Long,
        val schoolName: String,
        val name: String,
        @field:Schema(description = "PENDING 대기, APPROVED 승인, REJECTED 거절(다시 신청 가능), REVOKED 권한 회수")
        val status: TeacherApplicationStatus,
        @field:Schema(description = "거절할 때 관리자가 적은 사유")
        val rejectReason: String?,
        @field:Schema(description = "이메일 인증을 마친 시각. 인증 기능을 붙이기 전에는 null")
        val emailVerifiedAt: LocalDateTime?,
        val reviewedAt: LocalDateTime?,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun of(dto: TeacherApplicationDto) = Application(
                id = dto.id,
                email = dto.email,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                name = dto.name,
                status = dto.status,
                rejectReason = dto.rejectReason,
                emailVerifiedAt = dto.emailVerifiedAt,
                reviewedAt = dto.reviewedAt,
                createdAt = dto.createdAt,
            )
        }
    }

    @Schema(name = "TeacherApplicationResponseMine")
    data class Mine(
        @field:Schema(description = "신청한 적이 없으면 null")
        val application: Application?,
    )
}
