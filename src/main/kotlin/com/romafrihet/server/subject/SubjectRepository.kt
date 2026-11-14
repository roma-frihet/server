package com.romafrihet.server.subject

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.data.jpa.repository.JpaRepository

interface SubjectRepository : JpaRepository<Subject, Long> {
    fun findAllBySchoolLevelOrderByDisplayOrderAsc(schoolLevel: SchoolLevel): List<Subject>
}

fun SubjectRepository.getOrThrow(id: Long): Subject =
    findById(id).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
