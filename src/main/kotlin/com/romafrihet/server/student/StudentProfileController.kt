package com.romafrihet.server.student

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.student.rqrs.StudentProfileRequest
import com.romafrihet.server.student.rqrs.StudentProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "학생 - 가입 정보")
@RestController
@RequestMapping("${ApiPaths.API_V1}/student-profiles")
class StudentProfileController(private val studentProfileService: StudentProfileService) : BaseController {
    @Operation(summary = "학생 가입 정보 저장", description = "Supabase 가입 직후 학교, 학번, 이름을 저장합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: StudentProfileRequest.Create): StudentProfileResponse.Result =
        StudentProfileResponse.Result.of(studentProfileService.create(request.toCommand(getUserId())))

    @Operation(summary = "내 가입 정보")
    @GetMapping("/me")
    fun findMine(): StudentProfileResponse.Mine =
        StudentProfileResponse.Mine.of(studentProfileService.find(getUserId()))
}
