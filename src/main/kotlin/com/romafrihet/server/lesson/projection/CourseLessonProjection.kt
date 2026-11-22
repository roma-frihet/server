package com.romafrihet.server.lesson.projection

/** 문제가 하나 이상 있는 (수업, 차시)다. 문제가 없는 차시는 열 수 없다. */
data class CourseLessonProjection(val courseId: Long, val lessonNumber: Int)
