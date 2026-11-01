package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.dto.LessonClassroomStatusDto
import com.romafrihet.server.lesson.dto.LessonDto
import io.swagger.v3.oas.annotations.media.Schema

class LessonResponse private constructor() {
    @Schema(name = "LessonResponseClassroomStatus")
    data class ClassroomStatus(val classroomId: Long, val classroomName: String, val status: LessonStatus) {
        companion object {
            fun of(dto: LessonClassroomStatusDto) = ClassroomStatus(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                status = dto.status,
            )
        }
    }

    @Schema(name = "LessonResponseResult")
    data class Result(
        val lessonNumber: Int,
        val title: String,
        val questionCount: Long,
        val classrooms: List<ClassroomStatus>,
    ) {
        companion object {
            fun of(dto: LessonDto) = Result(
                lessonNumber = dto.lessonNumber,
                title = dto.title,
                questionCount = dto.questionCount,
                classrooms = dto.classrooms.map(ClassroomStatus::of),
            )
        }
    }
}
