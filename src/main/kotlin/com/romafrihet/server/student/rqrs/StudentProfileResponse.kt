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
