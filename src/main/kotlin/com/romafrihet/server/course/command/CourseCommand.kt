package com.romafrihet.server.course.command

import java.util.UUID

class CourseCommand private constructor() {
    data class Create(val userId: UUID, val schoolId: Long, val grade: Int, val subject: String)
}
