package com.romafrihet.server.lesson.command

import java.util.UUID

class LessonOpeningCommand private constructor() {
    data class Change(val userId: UUID, val classroomId: Long, val lessonNumber: Int)
}
