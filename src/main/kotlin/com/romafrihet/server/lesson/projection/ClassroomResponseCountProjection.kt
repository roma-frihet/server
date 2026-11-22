package com.romafrihet.server.lesson.projection

/** 한 차시에서 하나라도 답한 학생 수다. 점수 격자(AttemptScoreProjection)의 answeredCount > 0과 같은 기준이다. */
data class ClassroomResponseCountProjection(val classroomId: Long, val responseCount: Long)
