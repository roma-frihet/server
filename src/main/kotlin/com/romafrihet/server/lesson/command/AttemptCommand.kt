package com.romafrihet.server.lesson.command

import java.util.UUID

class AttemptCommand private constructor() {
    data class Query(val userId: UUID, val classroomId: Long, val lessonNumber: Int)

    data class Start(val userId: UUID, val classroomId: Long, val lessonNumber: Int, val questionIndex: Int)

    data class Answer(
        val userId: UUID,
        val classroomId: Long,
        val lessonNumber: Int,
        val questionIndex: Int,
        val optionId: Long?,
        val text: String?,
    )

    data class DeleteAllInClassroom(val classroomId: Long, val userId: UUID)
}
