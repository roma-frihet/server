package com.romafrihet.server.lesson.rqrs

import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.dto.LessonClassroomStatusDto
import com.romafrihet.server.lesson.dto.LessonDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

class LessonResponse private constructor() {
    @Schema(name = "LessonResponseClassroomStatus")
    data class ClassroomStatus(
        val classroomId: Long,
        val classNo: Int,
        val status: LessonStatus,
        @field:Schema(description = "열기 예약 시각(시간대 포함). 없으면 null")
        val scheduledOpenAt: OffsetDateTime?,
        @field:Schema(description = "자동 마감 시각(시간대 포함). 없으면 null")
        val scheduledFinishAt: OffsetDateTime?,
    ) {
        companion object {
            fun of(dto: LessonClassroomStatusDto) = ClassroomStatus(
                classroomId = dto.classroomId,
                classNo = dto.classNo,
                status = dto.status,
                scheduledOpenAt = dto.scheduledOpenAt,
                scheduledFinishAt = dto.scheduledFinishAt,
            )
        }
    }

    @Schema(name = "LessonResponseResult")
    data class Result(
        val lessonNumber: Int,
        val title: String,
        val questionCount: Long,
        @field:Schema(description = "이 수업의 모든 반을 합쳐 이 차시를 푼 적이 있는 학생 수. 지우면 점수가 바뀌는 인원이다")
        val attemptedStudentCount: Long,
        val classrooms: List<ClassroomStatus>,
    ) {
        companion object {
            fun of(dto: LessonDto) = Result(
                lessonNumber = dto.lessonNumber,
                title = dto.title,
                questionCount = dto.questionCount,
                attemptedStudentCount = dto.attemptedStudentCount,
                classrooms = dto.classrooms.map(ClassroomStatus::of),
            )
        }
    }
}
