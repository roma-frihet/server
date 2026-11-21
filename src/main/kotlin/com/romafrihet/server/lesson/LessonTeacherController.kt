package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.rqrs.LessonRequest
import com.romafrihet.server.lesson.rqrs.LessonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 차시")
@RestController
@RequestMapping("${ApiPaths.TEACHER_V1}/courses/{courseId}/lessons")
class LessonTeacherController(private val lessonService: LessonService) : BaseController {
    @Operation(
        operationId = "findLessons",
        summary = "차시 목록",
        description = "차시별 문제 수와 반별 상태를 함께 내립니다.",
    )
    @GetMapping
    fun findAll(@PathVariable courseId: Long): List<LessonResponse.Result> =
        lessonService.findAll(LessonCommand.Query(getUserId(), courseId)).map(LessonResponse.Result::of)

    @Operation(
        operationId = "createLesson",
        summary = "차시 추가",
        description = "차시 번호는 마지막 번호 다음으로 정해집니다.",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable courseId: Long, @Valid @RequestBody request: LessonRequest.Save): LessonResponse.Result =
        LessonResponse.Result.of(lessonService.create(request.toCreateCommand(getUserId(), courseId)))

    @Operation(
        operationId = "renameLesson",
        summary = "차시 이름 수정",
    )
    @PatchMapping("/{lessonNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rename(
        @PathVariable courseId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @RequestBody request: LessonRequest.Save,
    ) = lessonService.rename(request.toRenameCommand(getUserId(), courseId, lessonNumber))

    @Operation(
        operationId = "deleteLesson",
        summary = "차시 삭제",
        description = "그 차시의 문제와 연 기록도 함께 지웁니다. 학생이 푼 적이 있으면 지울 수 없습니다.",
    )
    @DeleteMapping("/{lessonNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable courseId: Long, @PathVariable lessonNumber: Int) =
        lessonService.delete(LessonCommand.Delete(getUserId(), courseId, lessonNumber))
}
