package com.romafrihet.server.teacher

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.teacher.command.TeacherCommand
import com.romafrihet.server.teacher.rqrs.TeacherRequest
import com.romafrihet.server.teacher.rqrs.TeacherResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 마이페이지")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class TeacherController(
    private val teacherService: TeacherService,
    private val studentPasswordResetService: StudentPasswordResetService,
) : BaseController {
    @Operation(
        operationId = "findMyPage",
        summary = "마이페이지",
        description = "이메일과 비밀번호는 Supabase Auth에서 다룹니다.",
    )
    @GetMapping("/mypage")
    fun findMyPage(): TeacherResponse.MyPage = TeacherResponse.MyPage.of(teacherService.findMyPage(getUserId()))

    @Operation(
        operationId = "saveTeacherProfile",
        summary = "내 정보 저장",
        description = "처음 저장과 수정에 함께 씁니다. 학교를 옮겨도 기존 수업은 그대로입니다.",
    )
    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveProfile(@Valid @RequestBody request: TeacherRequest.SaveProfile) =
        teacherService.saveProfile(request.toCommand(getUserId()))

    @Operation(
        operationId = "deleteCourse",
        summary = "수업 삭제",
        description = "반이나 문제가 있으면 삭제할 수 없습니다.",
    )
    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCourse(@PathVariable courseId: Long) =
        teacherService.deleteCourse(TeacherCommand.DeleteCourse(getUserId(), courseId))

    @Operation(
        operationId = "deleteClassroom",
        summary = "반 삭제",
        description = "학생이나 차시 기록이 있으면 삭제할 수 없습니다.",
    )
    @DeleteMapping("/classrooms/{classroomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteClassroom(@PathVariable classroomId: Long) =
        teacherService.deleteClassroom(TeacherCommand.DeleteClassroom(getUserId(), classroomId))

    @Operation(
        operationId = "resetStudentPassword",
        summary = "학생 비밀번호 초기화",
        description = "임시 비밀번호를 만들어 한 번만 내립니다. 학생의 기존 세션은 무효가 됩니다.",
    )
    @PostMapping("/classrooms/{classroomId}/students/{membershipId}/password-reset")
    fun resetStudentPassword(
        @PathVariable classroomId: Long,
        @PathVariable membershipId: Long,
    ): TeacherResponse.TemporaryPassword = TeacherResponse.TemporaryPassword(
        studentPasswordResetService.reset(
            TeacherCommand.ResetStudentPassword(getUserId(), classroomId, membershipId),
        ),
    )
}
