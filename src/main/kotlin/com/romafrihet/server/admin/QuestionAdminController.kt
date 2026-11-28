package com.romafrihet.server.admin

import com.romafrihet.server.admin.rqrs.AdminRequest
import com.romafrihet.server.admin.rqrs.AdminResponse
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.lesson.QuestionService
import com.romafrihet.server.lesson.command.QuestionCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 - 문제")
@RestController
@RequestMapping("${ApiPaths.ADMIN_V1}/questions")
class QuestionAdminController(
    private val questionAdminService: QuestionAdminService,
    private val questionService: QuestionService,
) {
    @Operation(
        operationId = "findAdminQuestions",
        summary = "전체 문제 목록",
        description = "모든 교사의 문제를 최근에 만든 순서로 내립니다.",
    )
    @GetMapping
    fun findAll(@Valid @ParameterObject query: AdminRequest.QuestionQuery): AdminResponse.QuestionResultPage =
        AdminResponse.QuestionResultPage.of(questionAdminService.findAll(query.toCommand()))

    @Operation(
        operationId = "deleteQuestionByAdmin",
        summary = "문제 삭제",
        description = "교사 삭제와 같은 규칙입니다. 학생이 푼 기록도 함께 지우고, 지금 열린 차시의 문제는 LS024로 막습니다.",
    )
    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable questionId: Long) = questionService.deleteAsAdmin(QuestionCommand.AdminDelete(questionId))
}
