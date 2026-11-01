package com.romafrihet.server.school

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import org.springframework.data.jpa.repository.JpaRepository

interface SchoolRepository : JpaRepository<School, Long> {
    fun findTop20ByNameContainingOrderByNameAsc(keyword: String): List<School>
}

fun SchoolRepository.getOrThrow(id: Long): School = findById(id).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
