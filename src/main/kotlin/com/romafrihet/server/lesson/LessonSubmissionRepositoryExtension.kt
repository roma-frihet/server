package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.dto.LessonScoreQueryDto
import com.romafrihet.server.lesson.projection.LessonScoreProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LessonSubmissionRepositoryExtension {
    fun queryScores(query: LessonScoreQueryDto, pageable: Pageable): Page<LessonScoreProjection>
}
