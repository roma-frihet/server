package com.romafrihet.server.course.rqrs

import com.romafrihet.server.course.dto.CourseDto
import io.swagger.v3.oas.annotations.media.Schema

class CourseResponse private constructor() {
    @Schema(name = "CourseResponseResult")
    data class Result(val id: Long, val schoolId: Long, val schoolName: String, val grade: Int, val subject: String) {
        companion object {
            fun of(dto: CourseDto) = Result(
                id = dto.id,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
                grade = dto.grade,
                subject = dto.subject,
            )
        }
    }
}
