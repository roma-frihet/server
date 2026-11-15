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
        /** 로그인에 쓴 계정이다. 학번 규칙과 맞는지 확인하는 데만 쓴다. */
        val loginEmail: String?,
    )
}
