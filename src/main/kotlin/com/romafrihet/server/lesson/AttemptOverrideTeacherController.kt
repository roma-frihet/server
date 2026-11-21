package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.rqrs.AttemptOverrideRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 결과 수정")
@RestController
@RequestMapping("${ApiPaths.TEACHER_V1}/attempt-answers")
class AttemptOverrideTeacherController(private val attemptOverrideService: AttemptOverrideService) : BaseController {
    @Operation(
        operationId = "overrideAttemptResult",
        summary = "문제 결과 덮어쓰기",
        description = "접속이 끊기는 등 사정이 있을 때 씁니다. 확정된 문제만 바꿀 수 있고 원래 결과는 이력으로 남습니다.",
    )
    @PutMapping("/{attemptAnswerId}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun override(@PathVariable attemptAnswerId: Long, @Valid @RequestBody request: AttemptOverrideRequest.Update) =
        attemptOverrideService.override(request.toCommand(getUserId(), attemptAnswerId))
}
