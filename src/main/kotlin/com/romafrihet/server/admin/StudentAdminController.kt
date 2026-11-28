package com.romafrihet.server.admin

import com.romafrihet.server.admin.rqrs.AdminRequest
import com.romafrihet.server.admin.rqrs.AdminResponse
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "관리자 - 학생")
@RestController
@RequestMapping("${ApiPaths.ADMIN_V1}/students")
class StudentAdminController(private val studentAdminService: StudentAdminService) : BaseController {
    @Operation(
        operationId = "findAdminStudents",
        summary = "학생 목록",
        description = "학생 가입 정보를 저장한 계정입니다. 학교, 학번 순입니다.",
    )
    @GetMapping
    fun findAll(@Valid @ParameterObject query: AdminRequest.StudentQuery): AdminResponse.StudentResultPage =
        AdminResponse.StudentResultPage.of(studentAdminService.findAll(query.toCommand()))

    @Operation(
        operationId = "resetStudentPasswordByAdmin",
        summary = "학생 비밀번호 초기화",
        description = "임시 비밀번호를 만들어 한 번만 내립니다. 학생의 기존 세션은 무효가 됩니다.",
    )
    @PostMapping("/{studentUserId}/password-reset")
    fun resetPassword(@PathVariable studentUserId: UUID): AdminResponse.TemporaryPassword =
        AdminResponse.TemporaryPassword(studentAdminService.resetPassword(studentUserId))
}
