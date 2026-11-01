package com.romafrihet.server.classroom.dto

import com.romafrihet.server.classroom.MembershipStatus
import java.time.LocalDateTime

data class ClassroomMembershipDto(
    val id: Long,
    val classroomId: Long,
    val classroomName: String,
    val grade: Int,
    val subject: String,
    val studentName: String,
    val studentNumber: String,
    val status: MembershipStatus,
    val requestedAt: LocalDateTime,
)
