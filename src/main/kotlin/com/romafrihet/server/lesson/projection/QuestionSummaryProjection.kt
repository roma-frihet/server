package com.romafrihet.server.lesson.projection

import com.romafrihet.server.lesson.QuestionType
import java.time.LocalDateTime
import java.util.UUID

/** 관리자 문제 목록 한 행이다. 교사 이름은 교사 기능에 있어 부르는 쪽이 채운다. */
data class QuestionSummaryProjection(
    val questionId: Long,
    val courseId: Long,
    val teacherUserId: UUID,
    val schoolName: String,
    val grade: Int,
    val subjectName: String,
    val lessonNumber: Int,
    val lessonTitle: String,
    val type: QuestionType,
    val content: String?,
    val imageId: Long?,
    val createdAt: LocalDateTime,
)
