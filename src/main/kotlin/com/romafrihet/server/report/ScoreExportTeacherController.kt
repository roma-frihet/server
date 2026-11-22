package com.romafrihet.server.report

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.report.rqrs.ReportRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 대시보드, 학생 관리")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class ScoreExportTeacherController(private val scoreExportService: ScoreExportService) : BaseController {
    /** 파일 이름을 헤더로 보내야 하므로 ResponseEntity를 쓴다. */
    @Operation(
        operationId = "exportScores",
        summary = "성적 내보내기",
        description =
        "엑셀에서 바로 열리는 CSV(UTF-8 BOM)를 내려받습니다. STUDENT_SCORES는 학생 × 차시 점수, " +
            "QUESTION_RATES는 차시의 문제별 정답률, ANSWER_SHEET는 차시의 학생 × 문제 정오표입니다.",
    )
    @GetMapping("/courses/{courseId}/scores/export", produces = [CSV])
    fun export(
        @PathVariable courseId: Long,
        @Valid @ParameterObject query: ReportRequest.ScoreExportQuery,
    ): ResponseEntity<ByteArray> {
        val file = scoreExportService.export(query.toCommand(getUserId(), courseId))
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(CSV))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.fileName, Charsets.UTF_8).build().toString(),
            ).body(file.content)
    }

    private companion object {
        const val CSV = "text/csv;charset=UTF-8"
    }
}
