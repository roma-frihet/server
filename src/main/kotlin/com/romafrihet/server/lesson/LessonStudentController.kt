package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.AttemptCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.rqrs.LessonOpeningResponse
import com.romafrihet.server.lesson.rqrs.LessonResultResponse
import com.romafrihet.server.lesson.rqrs.QuestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학생 - 차시 문제")
@RestController
@RequestMapping("${ApiPaths.API_V1}/classrooms/{classroomId}/lessons")
class LessonStudentController(
    private val lessonStudentService: LessonStudentService,
    private val attemptService: AttemptService,
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

    @Operation(summary = "마감 차시의 문제, 내 답, 정답과 해설")
    @GetMapping("/{lessonNumber}/result")
    fun findResult(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int): LessonResultResponse.Result =
        LessonResultResponse.Result.of(
            attemptService.findResult(AttemptCommand.Query(getUserId(), classroomId, lessonNumber)),
        )
}
