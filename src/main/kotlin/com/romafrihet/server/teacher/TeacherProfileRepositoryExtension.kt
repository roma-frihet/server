package com.romafrihet.server.teacher

import com.romafrihet.server.teacher.dto.TeacherProfileQueryDto
import com.romafrihet.server.teacher.projection.TeacherProfileSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TeacherProfileRepositoryExtension {
    fun queryProfiles(query: TeacherProfileQueryDto, pageable: Pageable): Page<TeacherProfileSummaryProjection>
}
