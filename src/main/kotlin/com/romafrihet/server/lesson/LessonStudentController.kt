package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.command.SubmissionCommand
import com.romafrihet.server.lesson.rqrs.LessonOpeningResponse
import com.romafrihet.server.lesson.rqrs.QuestionResponse
import com.romafrihet.server.lesson.rqrs.SubmissionRequest
import com.romafrihet.server.lesson.rqrs.SubmissionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학생 - 차시 문제")
@RestController
@RequestMapping("${ApiPaths.API_V1}/classrooms/{classroomId}/lessons")
class LessonStudentController(
    private val lessonStudentService: LessonStudentService,
    private val lessonSubmissionService: LessonSubmissionService,
) : BaseController {
    @Operation(summary = "내 반의 열린 차시와 마감 차시 목록")
    @GetMapping
    fun findOpenedLessons(@PathVariable classroomId: Long): List<LessonOpeningResponse.OpenedResult> =
        lessonStudentService.findOpenedLessons(getUserId(), classroomId).map(LessonOpeningResponse.OpenedResult::of)

    @Operation(summary = "열린 차시의 문제 조회")
    @GetMapping("/{lessonNumber}/questions")
    fun findQuestions(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
    ): List<QuestionResponse.StudentResult> = lessonStudentService
        .findQuestions(QuestionCommand.StudentQuery(getUserId(), classroomId, lessonNumber))
        .map(QuestionResponse.StudentResult::of)

    @Operation(summary = "답안 제출", description = "차시마다 한 번만 제출합니다. 점수는 마감 후에 공개합니다.")
    @PostMapping("/{lessonNumber}/submission")
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @RequestBody request: SubmissionRequest.Create,
    ): QuestionResponse.IdResult = QuestionResponse.IdResult(
        lessonSubmissionService.submit(request.toCommand(getUserId(), classroomId, lessonNumber)),
    )

    @Operation(summary = "마감 차시의 문제, 내 답, 정답")
    @GetMapping("/{lessonNumber}/result")
    fun findResult(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int): SubmissionResponse.Result =
        SubmissionResponse.Result.of(
            lessonSubmissionService.findResult(SubmissionCommand.Query(getUserId(), classroomId, lessonNumber)),
        )
}
