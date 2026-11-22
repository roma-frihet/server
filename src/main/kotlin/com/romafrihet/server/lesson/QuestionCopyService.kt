package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.QuestionCopyDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 문제를 다른 차시나 수업으로 복사한다. 복사본은 원본과 따로 고쳐지고, 학생 풀이 기록은 따라가지 않는다.
 * 이미지는 같은 행을 함께 쓴다. 지울 때는 QuestionImageCleaner가 남은 참조를 보고 지우므로 원본이 지워져도 남는다.
 */
@Service
@Transactional(readOnly = true)
class QuestionCopyService(
    private val courseRepository: CourseRepository,
    private val lessonRepository: LessonRepository,
    private val questionRepository: QuestionRepository,
    private val lessonEditGuard: LessonEditGuard,
    private val lessonService: LessonService,
) {
    @Transactional
    fun copy(command: QuestionCommand.Copy): QuestionCopyDto {
        courseRepository.getOwned(command.courseId, command.userId)
        val targetCourse = courseRepository.getOwned(command.targetCourseId, command.userId)
        val source = lessonRepository.getByNumber(command.courseId, command.lessonNumber)
        val questions = selected(command)
        assertCheck(questions.isNotEmpty()) { ErrorCode.QUESTION_COPY_EMPTY }
        assertCheck(questions.size <= MAX_COPY_COUNT) { ErrorCode.INVALID_VALUE }
        val targetLessonNumber =
            command.targetLessonNumber?.also {
                lessonRepository.getByNumber(command.targetCourseId, it)
                lessonEditGuard.validateCanAddQuestions(command.targetCourseId, it)
            }
                ?: lessonService.create(
                    LessonCommand.Create(command.userId, command.targetCourseId, source.title),
                ).lessonNumber

        val firstOrder = questionRepository.nextDisplayOrder(command.targetCourseId, targetLessonNumber)
        questionRepository.saveAll(
            questions.mapIndexed { index, question ->
                Question.create(
                    targetCourse,
                    firstOrder + index,
                    toCreate(question, targetCourse, command.userId, targetLessonNumber),
                )
            },
        )
        return QuestionCopyDto(
            targetCourseId = command.targetCourseId,
            targetLessonNumber = targetLessonNumber,
            copiedCount = questions.size,
        )
    }

    /**
     * 고른 문제가 이 차시의 문제가 아니면 404다. 다른 교사나 다른 차시의 문제를 끌어오지 못하게 하려고
     * 차시의 문제를 읽어 고른 id와 맞춰 본다. 범위가 한 차시라 크기가 작다.
     */
    private fun selected(command: QuestionCommand.Copy): List<Question> {
        val questions = questionRepository.findAllWithOptions(command.courseId, command.lessonNumber)
        if (command.questionIds.isEmpty()) return questions
        val wanted = command.questionIds.toSet()
        val picked = questions.filter { it.id in wanted }
        assertCheck(picked.size == wanted.size) { ErrorCode.NOT_FOUND }
        return picked
    }

    private fun toCreate(question: Question, targetCourse: Course, userId: UUID, lessonNumber: Int) =
        QuestionCommand.Create(
            userId = userId,
            courseId = requireNotNull(targetCourse.id),
            lessonNumber = lessonNumber,
            type = question.type,
            content = question.content,
            imageId = question.imageId,
            timeLimitSec = question.timeLimitSec,
            explanation = question.explanation,
            options =
            question.options
                .sortedBy { it.displayOrder }
                .map { QuestionCommand.Option(it.content, it.imageId, it.isCorrect) },
        )

    private companion object {
        /** 한 번에 복사할 수 있는 문제 수다. 문제를 골랐든 차시 전체든 같은 상한을 쓴다. */
        const val MAX_COPY_COUNT = 100
    }
}
