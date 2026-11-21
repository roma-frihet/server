package com.romafrihet.server.subject.rqrs

import com.romafrihet.server.subject.SchoolLevel
import com.romafrihet.server.subject.dto.SubjectDto
import io.swagger.v3.oas.annotations.media.Schema

class SubjectResponse private constructor() {
    @Schema(name = "SubjectResponseResult")
    data class Result(val id: Long, val schoolLevel: SchoolLevel, val subjectGroup: String, val name: String) {
        companion object {
            fun of(dto: SubjectDto) = Result(
                id = dto.id,
                schoolLevel = dto.schoolLevel,
                subjectGroup = dto.subjectGroup,
                name = dto.name,
            )
        }
    }
}
