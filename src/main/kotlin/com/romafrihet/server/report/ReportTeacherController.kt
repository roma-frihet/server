package com.romafrihet.server.report

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.rqrs.ReportRequest
import com.romafrihet.server.report.rqrs.ReportResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 대시보드, 학생 관리")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class ReportTeacherController(private val reportService: ReportService) : BaseController {
    @Operation(summary = "수업 대시보드", description = "시작 체크리스트, 열린 문제, 반별 진도와 평균, 살펴볼 학생을 내립니다.")
    @GetMapping("/courses/{courseId}/dashboard")
    fun findDashboard(@PathVariable courseId: Long): ReportResponse.Dashboard =
        ReportResponse.Dashboard.of(reportService.findDashboard(ReportCommand.Dashboard(getUserId(), courseId)))

    @Operation(
        summary = "학생 점수 조회",
        description = "반과 차시로 거를 수 있습니다. 열었던 차시마다 한 행이고, 미제출 학생은 correctCount가 null입니다.",
    )
    @GetMapping("/courses/{courseId}/scores")
    fun findScores(
        @PathVariable courseId: Long,
        @Valid @ParameterObject query: ReportRequest.ScoreQuery,
    ): ReportResponse.ScoreResultPage =
        ReportResponse.ScoreResultPage.of(reportService.findScores(query.toCommand(getUserId(), courseId)))

    @Operation(summary = "반 학생 관리", description = "승인된 학생의 점수와 상태를 내립니다. 검색어는 학생 목록에만 적용합니다.")
    @GetMapping("/classrooms/{classroomId}/students")
    fun findStudents(
        @PathVariable classroomId: Long,
        @Valid @ParameterObject query: ReportRequest.StudentQuery,
    ): ReportResponse.Classroom =
        ReportResponse.Classroom.of(reportService.findStudents(query.toCommand(getUserId(), classroomId)))
}
