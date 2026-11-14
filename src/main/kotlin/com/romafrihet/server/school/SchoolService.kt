package com.romafrihet.server.school

import com.romafrihet.server.school.dto.SchoolDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SchoolService(private val schoolRepository: SchoolRepository) {
    fun findAll(keyword: String): List<SchoolDto> = schoolRepository
        .findTop20ByNameContainingOrderByNameAsc(keyword)
        .map { SchoolDto(id = requireNotNull(it.id), name = it.name) }
}
