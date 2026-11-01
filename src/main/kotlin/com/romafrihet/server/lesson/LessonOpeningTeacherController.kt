package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.rqrs.LessonOpeningResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 차시 열기")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class LessonOpeningTeacherController(private val lessonOpeningService: LessonOpeningService) : BaseController {
    @Operation(summary = "반별 현재 차시와 열림 현황")
    @GetMapping("/lesson-openings")
    fun findStatuses(): List<LessonOpeningResponse.Result> =
        lessonOpeningService.findStatuses(getUserId()).map(LessonOpeningResponse.Result::of)

    @Operation(summary = "차시 열기", description = "닫았던 차시도 같은 API로 다시 엽니다.")
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun open(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.open(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))

    @Operation(summary = "차시 마감", description = "마감하면 다시 열 수 없고, 학생은 내 답과 정답을 볼 수 있습니다.")
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun finish(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.finish(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))

    @Operation(summary = "차시 닫기", description = "학생에게 숨깁니다. 다시 열 수 있습니다.")
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun close(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int) =
        lessonOpeningService.close(LessonOpeningCommand.Change(getUserId(), classroomId, lessonNumber))
}
