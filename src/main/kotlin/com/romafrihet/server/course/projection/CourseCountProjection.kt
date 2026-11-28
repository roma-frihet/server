package com.romafrihet.server.course.projection

import java.util.UUID

data class CourseCountProjection(val userId: UUID, val courseCount: Long)
