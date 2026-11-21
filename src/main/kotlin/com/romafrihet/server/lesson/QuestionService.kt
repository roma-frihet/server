package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.QuestionDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val courseRepository: CourseRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionContentValidator: QuestionContentValidator,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    private val questionImageCleaner: QuestionImageCleaner,
    private val lessonRepository: LessonRepository,
    private val attemptRepository: AttemptRepository,
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
        val displayOrder = questionRepository.nextDisplayOrder(command.courseId, command.lessonNumber)
        return requireNotNull(questionRepository.save(Question.create(course, displayOrder, command)).id)
    }

    @Transactional
    fun update(command: QuestionCommand.Update) {
        val question = questionRepository.getOwned(command.questionId, command.userId)
        val courseId = requireNotNull(question.course.id)
        questionContentValidator.validate(
            userId = command.userId,
            type = command.type,
            content = command.content,
            imageId = command.imageId,
            options = command.options,
        )

        if (attemptRepository.existsByClassroomCourseIdAndLessonNumber(courseId, question.lessonNumber)) {
            assertCheck(!question.hasScoringChange(command)) { ErrorCode.QUESTION_LOCKED_BY_SUBMISSION }
            question.updateTextOnly(command)
            return
        }

        val previousImages = question.imageIds()
        question.update(command)
        questionImageCleaner.removeUnused(previousImages - question.imageIds().toSet())
    }

    @Transactional
    fun delete(command: QuestionCommand.Delete) {
        val question = questionRepository.getOwned(command.questionId, command.userId)
        validateNoAttempts(requireNotNull(question.course.id), question.lessonNumber)

        val images = question.imageIds()
        questionRepository.delete(question)
        questionImageCleaner.removeUnused(images)
    }

    private fun validateLessonNumber(courseId: Long, lessonNumber: Int) {
        if (!lessonRepository.existsByCourseIdAndLessonNumber(courseId, lessonNumber)) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }
    }

    /**
     * 문제를 더하거나 내용을 바꿀 때의 조건. 이미 연 차시에 문제가 늘면 점수의 분모가 달라져
     * 먼저 푼 학생과 나중에 푼 학생을 비교할 수 없게 된다.
     */
    private fun validateEditable(courseId: Long, lessonNumber: Int) {
        val locked =
            lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId, lessonNumber) ||
                lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(
                    courseId,
                    lessonNumber,
                ) ||
                attemptRepository.existsByClassroomCourseIdAndLessonNumber(courseId, lessonNumber)
        assertCheck(!locked) { ErrorCode.LESSON_LOCKED_BY_OPENING }
    }

    /**
     * 지울 때의 조건. 더하는 것과 달리 연 적이 있다는 것만으로는 막지 않는다. 아무도 풀지
     * 않았다면 지워도 틀어질 기록이 없다. 푼 사람이 있으면 분모가 줄어 이미 매긴 점수가
     * 거짓이 되므로 그때만 막는다.
     */
    private fun validateNoAttempts(courseId: Long, lessonNumber: Int) {
        assertCheck(!attemptRepository.existsByClassroomCourseIdAndLessonNumber(courseId, lessonNumber)) {
            ErrorCode.LESSON_HAS_ATTEMPTS
        }
    }
}
