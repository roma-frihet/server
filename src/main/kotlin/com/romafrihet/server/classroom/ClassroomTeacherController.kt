package com.romafrihet.server.classroom

import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.classroom.rqrs.ClassroomMembershipResponse
import com.romafrihet.server.classroom.rqrs.ClassroomRequest
import com.romafrihet.server.classroom.rqrs.ClassroomResponse
import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 반")
@RestController
@RequestMapping("${ApiPaths.TEACHER_V1}/classrooms")
class ClassroomTeacherController(
    private val classroomService: ClassroomService,
    private val classroomMembershipService: ClassroomMembershipService,
) : BaseController {
    @Operation(
        operationId = "createClassroom",
        summary = "반 생성",
        description = "반은 수업에 속합니다. 합류 코드는 수업 + 반마다 발급합니다.",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: ClassroomRequest.Create): ClassroomResponse.Result =
        ClassroomResponse.Result.of(classroomService.create(request.toCommand(getUserId())))

    @Operation(
        operationId = "findClassrooms",
        summary = "내 반 목록",
    )
    @GetMapping
    fun findAll(): List<ClassroomResponse.Result> =
        classroomService.findAll(getUserId()).map(ClassroomResponse.Result::of)

    @Operation(
        operationId = "issueJoinCode",
        summary = "합류 코드 발급",
        description = "코드는 5분간 유효하고, 다시 발급하면 이전 코드는 바로 무효가 됩니다.",
    )
    @PostMapping("/{classroomId}/join-code")
    fun issueJoinCode(@PathVariable classroomId: Long): ClassroomResponse.Result = ClassroomResponse.Result.of(
        classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(getUserId(), classroomId)),
    )

    @Operation(
        operationId = "closeJoinCode",
        summary = "합류 코드 끄기",
        description = "이미 합류한 학생은 그대로 유지됩니다.",
    )
    @DeleteMapping("/{classroomId}/join-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun closeJoinCode(@PathVariable classroomId: Long) =
        classroomService.closeJoinCode(ClassroomCommand.IssueJoinCode(getUserId(), classroomId))

    @Operation(
        operationId = "findClassroomMemberships",
        summary = "반 학생 목록",
        description = "코드로 연결된 학생입니다. 승인 절차는 없습니다.",
    )
    @GetMapping("/{classroomId}/memberships")
    fun findMemberships(@PathVariable classroomId: Long): List<ClassroomMembershipResponse.Result> =
        classroomMembershipService
            .findAllForTeacher(ClassroomMembershipCommand.Query(getUserId(), classroomId))
            .map(ClassroomMembershipResponse.Result::of)

    @Operation(
        operationId = "removeClassroomStudent",
        summary = "학생 내보내기",
        description = "코드를 잘못 넣어 들어온 학생을 반에서 뺍니다. 그 반에서 푼 기록도 함께 지워집니다.",
    )
    @DeleteMapping("/{classroomId}/memberships/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeStudent(@PathVariable classroomId: Long, @PathVariable membershipId: Long) =
        classroomMembershipService.removeStudent(
            ClassroomMembershipCommand.RemoveStudent(getUserId(), classroomId, membershipId),
        )
}
