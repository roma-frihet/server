package com.romafrihet.server.course

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.course.rqrs.CourseRequest
import com.romafrihet.server.course.rqrs.CourseResponse
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

@Tag(name = "교사 - 수업")
@RestController
@RequestMapping("${ApiPaths.TEACHER_V1}/courses")
class CourseTeacherController(private val courseService: CourseService) : BaseController {
    @Operation(summary = "수업 생성", description = "같은 과목이라도 학년이 다르면 다른 수업입니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CourseRequest.Create): CourseResponse.Result =
        CourseResponse.Result.of(courseService.create(request.toCommand(getUserId())))

    @Operation(summary = "내 수업 목록")
    @GetMapping
    fun findAll(): List<CourseResponse.Result> = courseService.findAll(getUserId()).map(CourseResponse.Result::of)
}
