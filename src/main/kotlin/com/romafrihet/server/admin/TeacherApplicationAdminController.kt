package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.admin.rqrs.AdminRequest
import com.romafrihet.server.admin.rqrs.AdminResponse
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 - 교사 가입 신청")
@RestController
@RequestMapping("${ApiPaths.ADMIN_V1}/teacher-applications")
class TeacherApplicationAdminController(private val teacherApplicationAdminService: TeacherApplicationAdminService) :
    BaseController {
    @Operation(
        operationId = "findTeacherApplications",
        summary = "교사 가입 신청 목록",
        description = "오래 기다린 신청부터 내립니다. status를 비우면 모든 상태를 봅니다.",
    )
    @GetMapping
    fun findAll(
        @Valid @ParameterObject query: AdminRequest.TeacherApplicationQuery,
    ): AdminResponse.TeacherApplicationResultPage =
        AdminResponse.TeacherApplicationResultPage.of(teacherApplicationAdminService.findAll(query.toCommand()))

    @Operation(
        operationId = "approveTeacherApplication",
        summary = "교사 가입 승인",
        description =
        "교사 역할을 주고 신청한 이름과 학교로 교사 프로필을 만듭니다. 거절했거나 권한을 거둔 계정도 승인할 수 있습니다. " +
            "이메일 인증을 요구하도록 켜 두었으면 인증하지 않은 신청은 TA003으로 거절합니다.",
    )
    @PostMapping("/{applicationId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approve(@PathVariable applicationId: Long) =
        teacherApplicationAdminService.approve(AdminCommand.ApproveTeacher(getUserId(), applicationId))

    @Operation(
        operationId = "rejectTeacherApplication",
        summary = "교사 가입 거절",
        description = "대기 중인 신청만 거절합니다. 신청한 사람은 사유를 보고 다시 신청할 수 있습니다.",
    )
    @PostMapping("/{applicationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reject(@PathVariable applicationId: Long, @Valid @RequestBody request: AdminRequest.RejectTeacher) =
        teacherApplicationAdminService.reject(request.toCommand(getUserId(), applicationId))
}
