package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.rqrs.AttemptOverrideRequest
import com.romafrihet.server.lesson.rqrs.AttemptOverrideResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 결과 수정")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class AnswerOverrideTeacherController(private val attemptOverrideService: AttemptOverrideService) : BaseController {
    @Operation(
        operationId = "overrideResultsByAnswer",
        summary = "주관식 같은 답 한 번에 인정",
        description =
        "이 반·차시·문제에서 같은 답(공백, 대소문자 무시)을 쓴 확정 응답의 결과를 한 번에 바꿉니다. " +
            "인정은 RIGHT, 취소는 WRONG입니다. 응답마다 수정 이력이 남습니다.",
    )
    @PostMapping("/classrooms/{classroomId}/lessons/{lessonNumber}/questions/{questionId}/answer-overrides")
    fun overrideByAnswer(
        @PathVariable classroomId: Long,
        @PathVariable lessonNumber: Int,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: AttemptOverrideRequest.ByAnswer,
    ): AttemptOverrideResponse.ByAnswer = AttemptOverrideResponse.ByAnswer(
        changedCount =
        attemptOverrideService.overrideByAnswer(
            request.toCommand(getUserId(), classroomId, lessonNumber, questionId),
        ),
    )
}
