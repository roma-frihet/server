package com.romafrihet.server.report

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.rqrs.StudentReportResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학생 - 리포트")
@RestController
@RequestMapping("${ApiPaths.API_V1}/classrooms/{classroomId}/report")
class ReportStudentController(private val studentReportService: StudentReportService) : BaseController {
    @Operation(
        operationId = "findMyReport",
        summary = "내 학습 리포트",
        description = "지금 열린 차시, 차시별 내 점수와 반 평균, 최근 틀린 문제를 내립니다. 다른 학생의 점수는 담지 않습니다.",
    )
    @GetMapping
    fun findReport(@PathVariable classroomId: Long): StudentReportResponse.Result = StudentReportResponse.Result.of(
        studentReportService.findReport(ReportCommand.StudentReport(getUserId(), classroomId)),
    )

    @Operation(
        operationId = "findMyWrongQuestions",
        summary = "틀린 문제 모음",
        description = "마감한 차시에서 맞히지 못한 문제를 최근 차시부터 내립니다.",
    )
    @GetMapping("/wrong-questions")
    fun findWrongQuestions(@PathVariable classroomId: Long): List<StudentReportResponse.WrongQuestion> =
        studentReportService
            .findWrongQuestions(ReportCommand.StudentReport(getUserId(), classroomId))
            .map(StudentReportResponse.WrongQuestion::of)
}
