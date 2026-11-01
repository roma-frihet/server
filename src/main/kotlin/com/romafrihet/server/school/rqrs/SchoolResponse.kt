package com.romafrihet.server.school.rqrs

import com.romafrihet.server.school.dto.SchoolDto
import io.swagger.v3.oas.annotations.media.Schema

class SchoolResponse private constructor() {
    @Schema(name = "SchoolResponseResult")
    data class Result(val id: Long, val name: String) {
        companion object {
            fun of(dto: SchoolDto) = Result(id = dto.id, name = dto.name)
        }
    }
}
