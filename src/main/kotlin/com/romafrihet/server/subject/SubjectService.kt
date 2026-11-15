package com.romafrihet.server.subject

import com.romafrihet.server.subject.dto.SubjectDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SubjectService(private val subjectRepository: SubjectRepository) {
    fun findAll(schoolLevel: SchoolLevel): List<SubjectDto> = subjectRepository
        .findAllBySchoolLevelOrderByDisplayOrderAsc(schoolLevel)
        .map {
            SubjectDto(
                id = requireNotNull(it.id),
                schoolLevel = it.schoolLevel,
                subjectGroup = it.subjectGroup,
                name = it.name,
            )
        }
}
