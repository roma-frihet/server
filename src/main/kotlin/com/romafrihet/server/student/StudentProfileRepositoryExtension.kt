package com.romafrihet.server.student

import com.romafrihet.server.student.dto.StudentProfileQueryDto
import com.romafrihet.server.student.projection.StudentProfileSummaryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StudentProfileRepositoryExtension {
    fun queryProfiles(query: StudentProfileQueryDto, pageable: Pageable): Page<StudentProfileSummaryProjection>
}
