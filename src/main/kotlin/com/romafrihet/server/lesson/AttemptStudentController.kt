package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.AttemptCommand
import com.romafrihet.server.lesson.rqrs.AttemptRequest
import com.romafrihet.server.lesson.rqrs.AttemptResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학생 - 문제 풀이")
@RestController
@RequestMapping("${ApiPaths.API_V1}/classrooms/{classroomId}/lessons/{lessonNumber}/attempt")
class AttemptStudentController(private val attemptService: AttemptService) : BaseController {
    @Operation(
        summary = "응시 상태",
        description = "제한 시간이 지난 문제를 먼저 확정한 뒤 상태를 내립니다. 시작하지 않은 문제는 개수와 제한 시간만 알려줍니다.",
    )
    @GetMapping
    fun findAttempt(@PathVariable classroomId: Long, @PathVariable lessonNumber: Int): AttemptResponse.Result =
        AttemptResponse.Result.of(
            attemptService.findAttempt(AttemptCommand.Query(getUserId(), classroomId, lessonNumber)),
        )

    @Operation(
        summary = "문제 시작",
        description = "앞 문제부터 순서대로만 시작합니다. 같은 문제로 다시 불러도 제한 시간이 늘어나지 않습니다.",
    )
    @PostMapping("/questions/{questionIndex}/start")
    fun start(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @PathVariable questionIndex: Int,
    ): AttemptResponse.Result = AttemptResponse.Result.of(
        attemptService.start(AttemptCommand.Start(getUserId(), classroomId, lessonNumber, questionIndex)),
    )

    @Operation(summary = "답 임시 저장", description = "제한 시간 안에서 마지막 값으로 덮어씁니다.")
    @PutMapping("/questions/{questionIndex}/draft")
    fun saveDraft(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @PathVariable questionIndex: Int,
        @Valid @RequestBody request: AttemptRequest.Answer,
    ): AttemptResponse.Result = AttemptResponse.Result.of(
        attemptService.saveDraft(request.toCommand(getUserId(), classroomId, lessonNumber, questionIndex)),
    )

    @Operation(
        summary = "답 제출",
        description = "결과와 정답, 해설을 함께 내립니다. 이미 확정된 문제는 확정 결과를 그대로 돌려줍니다.",
    )
    @PostMapping("/questions/{questionIndex}/submit")
    fun submit(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @PathVariable questionIndex: Int,
        @Valid @RequestBody request: AttemptRequest.Answer,
    ): AttemptResponse.Result = AttemptResponse.Result.of(
        attemptService.submit(request.toCommand(getUserId(), classroomId, lessonNumber, questionIndex)),
    )
}
