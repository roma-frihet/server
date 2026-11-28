package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.dto.QuestionAdminQueryDto
import com.romafrihet.server.lesson.projection.QuestionSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface QuestionRepositoryExtension {
    fun queryForAdmin(query: QuestionAdminQueryDto, pageable: Pageable): Page<QuestionSummaryProjection>
}
