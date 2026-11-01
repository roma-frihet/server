package com.romafrihet.server.classroom.command

import java.util.UUID

class ClassroomCommand private constructor() {
    data class Create(val userId: UUID, val courseId: Long, val name: String)

    data class IssueJoinCode(val userId: UUID, val classroomId: Long)
}
