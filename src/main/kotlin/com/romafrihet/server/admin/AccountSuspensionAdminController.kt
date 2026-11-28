package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.admin.rqrs.AdminRequest
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "관리자 - 계정 정지")
@RestController
@RequestMapping("${ApiPaths.ADMIN_V1}/users/{userId}/suspension")
class AccountSuspensionAdminController(private val accountSuspensionService: AccountSuspensionService) :
    BaseController {
    @Operation(
        operationId = "suspendAccount",
        summary = "계정 정지",
        description =
        "교사와 학생 모두 씁니다. 로그인과 토큰 갱신을 막고, 이미 받은 토큰은 만료(최대 1시간)까지 남습니다. " +
            "이미 정지한 계정에 다시 부르면 사유만 바꿉니다.",
    )
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun suspend(@PathVariable userId: UUID, @Valid @RequestBody request: AdminRequest.Suspend) =
        accountSuspensionService.suspend(request.toCommand(getUserId(), userId))

    @Operation(operationId = "unsuspendAccount", summary = "계정 정지 해제")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unsuspend(@PathVariable userId: UUID) =
        accountSuspensionService.unsuspend(AdminCommand.Unsuspend(getUserId(), userId))
}
