package com.romafrihet.server.student.rqrs

import io.swagger.v3.oas.annotations.media.Schema

class StudentPasswordResponse private constructor() {
    /** 임시 비밀번호는 이 응답에만 담긴다. 다시 조회할 수 없으니 교사가 바로 학생에게 알려 줘야 한다. */
    @Schema(name = "StudentPasswordResponseTemporary")
    data class Temporary(val temporaryPassword: String)
}
