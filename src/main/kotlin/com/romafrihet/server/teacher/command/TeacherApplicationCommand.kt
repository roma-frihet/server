package com.romafrihet.server.teacher.command

import java.util.UUID

class TeacherApplicationCommand private constructor() {
    /** email은 토큰에서 꺼낸 값이다. 학생 계정과 이메일이 없는 계정을 거르려고 nullable로 받는다. */
    data class Apply(val userId: UUID, val email: String?, val schoolId: Long, val name: String)
}
