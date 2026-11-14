package com.romafrihet.server.lesson

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QuestionImageRepository : JpaRepository<QuestionImage, Long> {
    fun countByIdInAndUserId(ids: Collection<Long>, userId: UUID): Long
}
