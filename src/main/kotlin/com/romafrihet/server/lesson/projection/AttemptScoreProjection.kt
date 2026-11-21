package com.romafrihet.server.lesson.projection

import java.util.UUID

/**
 * (학생 × 차시) 격자다. 점수는 저장하지 않으므로 읽을 때마다 여기서 센다.
 * answeredCount가 0이면 응시는 있지만 아무것도 답하지 않은 것이라 미응답으로 본다.
 */
data class AttemptScoreProjection(
    val classroomId: Long,
    val userId: UUID,
    val lessonNumber: Int,
    val correctCount: Long,
    val answeredCount: Long,
)
