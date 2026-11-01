package com.romafrihet.server.teacher.command

import java.util.UUID

class TeacherCommand private constructor() {
    data class SaveProfile(val userId: UUID, val schoolId: Long, val name: String)

    data class DeleteCourse(val userId: UUID, val courseId: Long)

    data class DeleteClassroom(val userId: UUID, val classroomId: Long)
}
