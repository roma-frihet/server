package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.rqrs.QuestionRequest
import com.romafrihet.server.lesson.rqrs.QuestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 문제")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class QuestionCopyTeacherController(private val questionCopyService: QuestionCopyService) : BaseController {
    @Operation(
        operationId = "copyQuestions",
        summary = "차시·문제 복사",
        description =
        "차시의 문제를 같은 수업이나 다른 수업으로 복사합니다. 대상 차시를 비우면 원본 제목으로 새 차시를 만듭니다. " +
            "문제를 골라 일부만 복사할 수 있고, 학생 풀이 기록은 복사하지 않습니다.",
    )
    @PostMapping("/courses/{courseId}/lessons/{lessonNumber}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    fun copy(
        @PathVariable courseId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @RequestBody request: QuestionRequest.Copy,
    ): QuestionResponse.CopyResult = QuestionResponse.CopyResult.of(
        questionCopyService.copy(request.toCommand(getUserId(), courseId, lessonNumber)),
    )
}
