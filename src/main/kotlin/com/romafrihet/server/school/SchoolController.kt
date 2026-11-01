package com.romafrihet.server.school

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.school.rqrs.SchoolRequest
import com.romafrihet.server.school.rqrs.SchoolResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학교")
@RestController
@RequestMapping(ApiPaths.SCHOOLS_V1)
class SchoolController(private val schoolService: SchoolService) {
    @Operation(summary = "학교 검색", description = "가입 전에 쓰므로 인증 없이 호출합니다. 최대 20개를 반환합니다.")
    @GetMapping
    fun findAll(@Valid @ParameterObject query: SchoolRequest.Query): List<SchoolResponse.Result> =
        schoolService.findAll(query.normalizedKeyword()).map(SchoolResponse.Result::of)
}
