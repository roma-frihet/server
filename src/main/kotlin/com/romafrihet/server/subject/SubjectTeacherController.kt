package com.romafrihet.server.subject

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.subject.rqrs.SubjectResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 과목")
@RestController
@RequestMapping("${ApiPaths.TEACHER_V1}/subjects")
class SubjectTeacherController(private val subjectService: SubjectService) {
    @Operation(
        operationId = "findSubjects",
        summary = "과목 목록",
        description = "수업을 만들 때 고를 수 있는 과목입니다. 학교급으로 거릅니다.",
    )
    @GetMapping
    fun findAll(@RequestParam(defaultValue = "MIDDLE") schoolLevel: SchoolLevel): List<SubjectResponse.Result> =
        subjectService.findAll(schoolLevel).map(SubjectResponse.Result::of)
}
