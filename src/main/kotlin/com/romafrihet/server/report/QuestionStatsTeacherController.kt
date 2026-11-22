package com.romafrihet.server.report

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.report.rqrs.QuestionStatsResponse
import com.romafrihet.server.report.rqrs.ReportRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 문제별 결과")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class QuestionStatsTeacherController(private val questionStatsService: QuestionStatsService) : BaseController {
    @Operation(
        operationId = "findQuestionStats",
        summary = "차시 문제별 결과",
        description =
        "반마다 차시 상태, 응답 수, 문제별 정답 수와 보기·주관식 답 분포를 내립니다. " +
            "classroomIds를 여러 개 주면 반 비교에, 하나만 주면 차시 상세와 수업 중 화면에 씁니다.",
    )
    @GetMapping("/courses/{courseId}/lessons/{lessonNumber}/question-stats")
    fun findQuestionStats(
        @PathVariable courseId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @ParameterObject query: ReportRequest.QuestionStatsQuery,
    ): List<QuestionStatsResponse.Classroom> = questionStatsService
        .findQuestionStats(query.toCommand(getUserId(), courseId, lessonNumber))
        .map(QuestionStatsResponse.Classroom::of)
}
