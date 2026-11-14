package com.romafrihet.server.lesson.command

import java.util.UUID

class SubmissionCommand private constructor() {
    data class Answer(val questionId: Long, val optionId: Long?, val text: String?)

    data class Create(val userId: UUID, val classroomId: Long, val lessonNumber: Int, val answers: List<Answer>)

    data class Query(val userId: UUID, val classroomId: Long, val lessonNumber: Int)
}
