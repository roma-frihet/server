package com.romafrihet.server.teacher

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.teacher.rqrs.TeacherApplicationRequest
import com.romafrihet.server.teacher.rqrs.TeacherApplicationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 가입 신청")
@RestController
@RequestMapping(ApiPaths.TEACHER_APPLICATIONS_V1)
class TeacherApplicationController(private val teacherApplicationService: TeacherApplicationService) :
    BaseController {
    @Operation(
        operationId = "applyTeacher",
        summary = "교사 가입 신청",
        description =
        "Supabase에 이메일로 가입한 뒤 부릅니다. 대기 중에 다시 부르면 내용을 고치고, 거절된 뒤 부르면 다시 신청합니다. " +
            "관리자가 승인하면 교사 역할이 생기므로 세션을 갱신해야 교사 API를 쓸 수 있습니다.",
    )
    @PutMapping("/me")
    fun apply(@Valid @RequestBody request: TeacherApplicationRequest.Apply): TeacherApplicationResponse.Application =
        TeacherApplicationResponse.Application.of(
            teacherApplicationService.apply(request.toCommand(getUserId(), getEmailOrNull())),
        )

    @Operation(
        operationId = "findMyTeacherApplication",
        summary = "내 교사 가입 신청",
        description = "신청한 적이 없으면 application이 null입니다. 승인되면 status가 APPROVED입니다.",
    )
    @GetMapping("/me")
    fun findMine(): TeacherApplicationResponse.Mine = TeacherApplicationResponse.Mine(
        teacherApplicationService.findMine(getUserId())?.let(TeacherApplicationResponse.Application::of),
    )
}
