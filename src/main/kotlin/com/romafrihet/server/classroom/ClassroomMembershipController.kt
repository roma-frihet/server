package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.rqrs.ClassroomMembershipRequest
import com.romafrihet.server.classroom.rqrs.ClassroomMembershipResponse
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
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

@Tag(name = "학생 - 반 합류")
@RestController
@RequestMapping("${ApiPaths.API_V1}/classroom-memberships")
class ClassroomMembershipController(private val classroomMembershipService: ClassroomMembershipService) :
    BaseController {
    @Operation(summary = "합류 코드로 반 합류 요청", description = "가입 때 저장한 이름과 학번으로 요청합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun join(@Valid @RequestBody request: ClassroomMembershipRequest.Join): ClassroomMembershipResponse.Result =
        ClassroomMembershipResponse.Result.of(classroomMembershipService.join(request.toCommand(getUserId())))

    @Operation(summary = "내 합류 요청과 소속 반 목록")
    @GetMapping
    fun findMine(): List<ClassroomMembershipResponse.Result> =
        classroomMembershipService.findAllForStudent(getUserId()).map(ClassroomMembershipResponse.Result::of)
}
