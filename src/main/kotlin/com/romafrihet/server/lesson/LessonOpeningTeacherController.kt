package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.rqrs.LessonOpeningRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 차시 열기")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class LessonOpeningTeacherController(private val lessonOpeningService: LessonOpeningService) : BaseController {
    @Operation(
        operationId = "openLesson",
        summary = "차시 열기",
        description = "닫았던 차시도 같은 API로 다시 엽니다.",
    )
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun open(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.open(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))

    @Operation(
        operationId = "finishLesson",
        summary = "차시 마감",
        description = "마감하면 다시 열 수 없고, 학생은 내 답과 정답을 볼 수 있습니다.",
    )
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun finish(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.finish(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))

    @Operation(
        operationId = "closeLesson",
        summary = "차시 닫기",
        description = "학생에게 숨깁니다. 다시 열 수 있습니다.",
    )
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun close(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.close(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))

    @Operation(
        operationId = "scheduleLesson",
        summary = "열기 예약, 자동 마감",
        description =
        "정한 시각에 차시를 열고 마감합니다. 이미 예약이 있으면 바꿉니다. 열린 차시는 finishAt만 보냅니다. " +
            "시각이 지난 뒤 이 반의 차시 상태를 처음 읽을 때 반영됩니다.",
    )
    @PutMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/schedule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun schedule(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @RequestBody request: LessonOpeningRequest.Schedule,
    ) = lessonOpeningService.schedule(request.toCommand(getUserId(), classroomId, lessonNumber))

    @Operation(
        operationId = "cancelLessonSchedule",
        summary = "예약 취소",
    )
    @DeleteMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/schedule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelSchedule(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.cancelSchedule(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))
}
