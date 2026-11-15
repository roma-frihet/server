package com.romafrihet.server.student.command

import java.util.UUID

class StudentPasswordCommand private constructor() {
    data class Change(val userId: UUID, val password: String)
}
