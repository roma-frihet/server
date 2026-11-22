package com.romafrihet.server.report

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.rqrs.StudentAnswerResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "교사 - 대시보드, 학생 관리")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class StudentAnswerTeacherController(private val studentAnswerService: StudentAnswerService) : BaseController {
    @Operation(
        operationId = "findStudentLessonAnswers",
        summary = "학생의 차시 풀이",
        description =
        "문제마다 학생이 고른 보기와 쓴 답, 정답, 결과를 내립니다. 시작하지 않은 문제는 NOT_STARTED, " +
            "푸는 중인 문제는 IN_PROGRESS입니다. 결과를 고칠 때는 attemptAnswerId를 씁니다.",
    )
    @GetMapping("/classrooms/{classroomId}/students/{membershipId}/lessons/{lessonNumber}/answers")
    fun findLessonAnswers(
        @PathVariable classroomId: Long,
        @PathVariable membershipId: Long,
        @PathVariable lessonNumber: Int,
    ): StudentAnswerResponse.Lesson = StudentAnswerResponse.Lesson.of(
        studentAnswerService.findLessonAnswers(
            ReportCommand.StudentLessonAnswers(getUserId(), classroomId, membershipId, lessonNumber),
        ),
    )
}
