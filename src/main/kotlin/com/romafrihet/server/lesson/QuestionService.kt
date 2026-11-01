package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.event.QuestionImagesRemovedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val questionImageRepository: QuestionImageRepository,
    private val courseRepository: CourseRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionContentValidator: QuestionContentValidator,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    private val eventPublisher: ApplicationEventPublisher,
    private val lessonRepository: LessonRepository,
    private val lessonSubmissionRepository: LessonSubmissionRepository,
) {
    /** 서명 URL 발급은 외부 호출이라 트랜잭션 없이 실행한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun findAll(command: QuestionCommand.Query): List<QuestionDto> {
        courseRepository.getOwned(command.courseId, command.userId)
        validateLessonNumber(command.courseId, command.lessonNumber)
        return questionImageUrlResolver.toDtos(
            questionRepository.findAllWithOptions(command.courseId, command.lessonNumber),
        )
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun find(command: QuestionCommand.Get): QuestionDto {
        val question =
            questionRepository.findWithOptions(command.questionId, command.userId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        return questionImageUrlResolver.toDtos(listOf(question)).single()
    }

    @Transactional
    fun create(command: QuestionCommand.Create): Long {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        validateLessonNumber(command.courseId, command.lessonNumber)
        validateEditable(command.courseId, command.lessonNumber)
        questionContentValidator.validate(
            userId = command.userId,
            type = command.type,
            content = command.content,
            imageId = command.imageId,
            options = command.options,
        )
        return requireNotNull(questionRepository.save(Question.create(course, command)).id)
    }

    @Transactional
    fun update(command: QuestionCommand.Update) {
        val question = questionRepository.getOwned(command.questionId, command.userId)
        validateEditable(requireNotNull(question.course.id), question.lessonNumber)
        questionContentValidator.validate(
            userId = command.userId,
            type = command.type,
            content = command.content,
            imageId = command.imageId,
            options = command.options,
        )

        val previousImages = question.imageIds()
        question.update(command)
        removeUnusedImages(previousImages - question.imageIds().toSet())
    }

    @Transactional
    fun delete(command: QuestionCommand.Delete) {
        val question = questionRepository.getOwned(command.questionId, command.userId)
        validateEditable(requireNotNull(question.course.id), question.lessonNumber)

        val images = question.imageIds()
        questionRepository.delete(question)
        removeUnusedImages(images)
    }

    /** 같은 이미지를 다른 문제나 보기가 쓰고 있을 수 있어, 변경을 반영한 뒤 참조가 없는 이미지만 지운다. */
    private fun removeUnusedImages(imageIds: List<Long>) {
        if (imageIds.isEmpty()) return
        questionRepository.flush()
        val used =
            questionRepository.findQuestionImageIds(imageIds) + questionRepository.findOptionImageIds(imageIds)
        val unused = questionImageRepository.findAllById(imageIds.toSet() - used.toSet())
        questionImageRepository.deleteAllInBatch(unused)
        if (unused.isNotEmpty()) eventPublisher.publishEvent(QuestionImagesRemovedEvent(unused.map { it.storagePath }))
    }

    private fun validateLessonNumber(courseId: Long, lessonNumber: Int) {
        if (!lessonRepository.existsByCourseIdAndLessonNumber(courseId, lessonNumber)) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }
    }

    /** 저장된 채점 결과와 어긋나지 않도록 열림, 마감, 제출이 있는 차시의 문제는 고치지 못한다. */
    private fun validateEditable(courseId: Long, lessonNumber: Int) {
        val locked =
            lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId, lessonNumber) ||
                lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(
                    courseId,
                    lessonNumber,
                ) ||
                lessonSubmissionRepository.existsByClassroomCourseIdAndLessonNumber(courseId, lessonNumber)
        assertCheck(!locked) { ErrorCode.LESSON_LOCKED_BY_OPENING }
    }
}
