package com.romafrihet.server.student.command

import java.util.UUID

class StudentProfileCommand private constructor() {
    data class Create(
        val userId: UUID,
        val schoolId: Long,
        val grade: Int,
        val classNumber: Int,
        val seatNumber: Int,
        val name: String,
    )
}
