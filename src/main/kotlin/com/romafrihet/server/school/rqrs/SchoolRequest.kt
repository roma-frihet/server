package com.romafrihet.server.school.rqrs

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

class SchoolRequest private constructor() {
    @Schema(name = "SchoolRequestQuery")
    data class Query(
        @field:Size(max = 100)
        val keyword: String? = null,
    ) {
        fun normalizedKeyword(): String = keyword?.trim().orEmpty()
    }
}
