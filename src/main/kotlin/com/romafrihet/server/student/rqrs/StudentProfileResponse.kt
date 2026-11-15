package com.romafrihet.server.student.rqrs

import com.romafrihet.server.student.dto.StudentProfileDto
import io.swagger.v3.oas.annotations.media.Schema

class StudentProfileResponse private constructor() {
    @Schema(name = "StudentProfileResponseResult")
    data class Result(
        val schoolId: Long,
        val schoolName: String,
        val grade: Int,
        val classNumber: Int,
        val seatNumber: Int,
        val studentNumber: String,
        val name: String,
        /** true면 교사가 초기화한 임시 비밀번호 상태다. 비밀번호 변경 화면으로 보낸다. */
        val mustChangePassword: Boolean,
    ) {
        companion object {
            fun of(dto: StudentProfileDto) = Result(
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                grade = dto.grade,
                classNumber = dto.classNumber,
                seatNumber = dto.seatNumber,
                studentNumber = dto.studentNumber,
                name = dto.name,
                mustChangePassword = dto.mustChangePassword,
            )
        }
    }

    @Schema(name = "StudentProfileResponseMine")
    data class Mine(@Schema(nullable = true) val profile: Result?) {
        companion object {
            fun of(dto: StudentProfileDto?) = Mine(profile = dto?.let(Result::of))
        }
    }
}
