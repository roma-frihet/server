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

    /** 반을 지울 때 그 반의 모든 학생 응시를 지운다. */
    data class DeleteAllInClassrooms(val classroomIds: Collection<Long>)

    /** 차시를 지울 때 그 차시의 모든 반, 모든 학생 응시를 지운다. */
    data class DeleteAllInLesson(val courseId: Long, val lessonNumber: Int)
}
