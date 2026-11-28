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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "관리자 - 교사")
@RestController
@RequestMapping("${ApiPaths.ADMIN_V1}/teachers")
class TeacherAdminController(private val teacherAdminService: TeacherAdminService) : BaseController {
    @Operation(
        operationId = "findAdminTeachers",
        summary = "교사 목록",
        description = "교사 프로필이 있는 계정입니다. 권한을 거둔 교사도 status=REVOKED로 남습니다.",
    )
    @GetMapping
    fun findAll(@Valid @ParameterObject query: AdminRequest.TeacherQuery): AdminResponse.TeacherResultPage =
        AdminResponse.TeacherResultPage.of(teacherAdminService.findAll(query.toCommand()))

    @Operation(
        operationId = "revokeTeacher",
        summary = "교사 권한 회수",
        description =
        "교사 역할을 지웁니다. 수업과 기록은 남고, 교사 가입 승인 API로 다시 권한을 줄 수 있습니다. " +
            "이미 받은 토큰은 만료(최대 1시간)될 때까지 쓸 수 있습니다.",
    )
    @PostMapping("/{teacherUserId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@PathVariable teacherUserId: UUID) =
        teacherAdminService.revoke(AdminCommand.RevokeTeacher(getUserId(), teacherUserId))
}
