package com.romafrihet.server.lesson

import com.romafrihet.server.common.web.ApiPaths
import com.romafrihet.server.common.web.BaseController
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.rqrs.QuestionRequest
import com.romafrihet.server.lesson.rqrs.QuestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "교사 - 문제")
@RestController
@RequestMapping(ApiPaths.TEACHER_V1)
class QuestionTeacherController(
    private val questionService: QuestionService,
    private val questionImageService: QuestionImageService,
) : BaseController {
    @Operation(summary = "차시의 문제 목록")
    @GetMapping("/courses/{courseId}/lessons/{lessonNumber}/questions")
    fun findQuestions(@PathVariable courseId: Long, @PathVariable lessonNumber: Int): List<QuestionResponse.Result> =
        questionService
            .findAll(QuestionCommand.Query(getUserId(), courseId, lessonNumber))
            .map(QuestionResponse.Result::of)

    @Operation(summary = "문제 추가")
    @PostMapping("/courses/{courseId}/lessons/{lessonNumber}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable courseId: Long,
        @PathVariable lessonNumber: Int,
        @Valid @RequestBody request: QuestionRequest.Save,
    ): QuestionResponse.IdResult =
        QuestionResponse.IdResult(questionService.create(request.toCreateCommand(getUserId(), courseId, lessonNumber)))

    @Operation(summary = "문제 상세")
    @GetMapping("/questions/{questionId}")
    fun find(@PathVariable questionId: Long): QuestionResponse.Result =
        QuestionResponse.Result.of(questionService.find(QuestionCommand.Get(getUserId(), questionId)))

    @Operation(summary = "문제 수정")
    @PutMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(@PathVariable questionId: Long, @Valid @RequestBody request: QuestionRequest.Save) =
        questionService.update(request.toUpdateCommand(getUserId(), questionId))

    @Operation(summary = "문제 삭제")
    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable questionId: Long) = questionService.delete(QuestionCommand.Delete(getUserId(), questionId))

    @Operation(
        summary = "문제 이미지 업로드",
        description = "PNG, JPEG, WebP, 5MB 이하. 응답의 id를 문제나 보기의 imageId에 넣어 저장하면 연결됩니다.",
    )
    @PostMapping("/question-images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadImage(@RequestPart file: MultipartFile): QuestionResponse.ImageResult = QuestionResponse.ImageResult.of(
        questionImageService.upload(QuestionCommand.UploadImage(getUserId(), file.bytes)),
    )
}
