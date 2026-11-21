package com.romafrihet.server.classroom.dto

import java.time.LocalDateTime

data class ClassroomMembershipDto(
    val id: Long,
    val classroomId: Long,
    val classNo: Int,
    val grade: Int,
    val subjectName: String,
    val studentName: String,
    val studentNumber: String,
    val joinedAt: LocalDateTime,
)
