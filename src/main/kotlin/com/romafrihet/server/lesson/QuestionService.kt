package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
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
    private val lessonEditGuard: LessonEditGuard,
    private val questionContentValidator: QuestionContentValidator,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    private val questionImageCleaner: QuestionImageCleaner,
    private val lessonRepository: LessonRepository,
    private val attemptRepository: AttemptRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonScheduleApplier: LessonScheduleApplier,
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
        lessonEditGuard.validateCanAddQuestions(command.courseId, command.lessonNumber)
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
        applyDueSchedules(courseId)
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
        applyDueSchedules(requireNotNull(question.course.id))
        validateNoAttempts(requireNotNull(question.course.id), question.lessonNumber)

        val images = question.imageIds()
        questionRepository.delete(question)
        questionImageCleaner.removeUnused(images)
    }

    /**
     * 열기 시각이 지났지만 아직 아무도 읽지 않아 적용되지 않은 예약을 먼저 적용한다.
     * 그러지 않으면 열려야 할 차시가 "열지 않은 차시"로 보여 문제를 고칠 수 있고, 그 뒤 예약이 조용히 사라진다.
     */
    private fun applyDueSchedules(courseId: Long) {
        lessonScheduleApplier.applyDue(
            classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId).map {
                requireNotNull(it.id)
            },
        )
    }

    private fun validateLessonNumber(courseId: Long, lessonNumber: Int) {
        if (!lessonRepository.existsByCourseIdAndLessonNumber(courseId, lessonNumber)) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }
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
