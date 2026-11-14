package com.romafrihet.server.lesson.command

import java.util.UUID

class LessonCommand private constructor() {
    data class Create(val userId: UUID, val courseId: Long, val title: String)

    data class Rename(val userId: UUID, val courseId: Long, val lessonNumber: Int, val title: String)

    data class Query(val userId: UUID, val courseId: Long)
}
