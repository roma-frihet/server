package com.romafrihet.server.lesson

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.QuestionCopyDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 문제를 다른 차시나 수업으로 복사한다. 복사본은 원본과 따로 고쳐지고, 학생 풀이 기록은 따라가지 않는다.
 * 이미지는 같은 행을 함께 쓴다. 지울 때는 QuestionImageCleaner가 남은 참조를 보고 지우므로 원본이 지워져도 남는다.
 */
@Service
@Transactional(readOnly = true)
class QuestionCopyService(
    private val courseRepository: CourseRepository,
    private val lessonRepository: LessonRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
) {
    @Transactional
    fun copy(command: QuestionCommand.Copy): QuestionCopyDto {
        courseRepository.getOwned(command.courseId, command.userId)
        val targetCourse = courseRepository.getOwned(command.targetCourseId, command.userId)
        val source = lessonRepository.getByNumber(command.courseId, command.lessonNumber)
        val questions = selected(command)
        val targetLessonNumber =
            command.targetLessonNumber?.also { validateTarget(command.targetCourseId, it) }
                ?: createLesson(command, source.title)

        val firstOrder = questionRepository.nextDisplayOrder(command.targetCourseId, targetLessonNumber)
        questionRepository.saveAll(
            questions.mapIndexed { index, question ->
                Question.create(targetCourse, firstOrder + index, toCreate(question, command, targetLessonNumber))
            },
        )
        return QuestionCopyDto(
            targetCourseId = command.targetCourseId,
            targetLessonNumber = targetLessonNumber,
            copiedCount = questions.size,
        )
    }

    /** 고른 문제가 이 차시의 문제가 아니면 404다. 다른 교사나 다른 차시의 문제를 끌어오지 못하게 한다. */
    private fun selected(command: QuestionCommand.Copy): List<Question> {
        val questions = questionRepository.findAllWithOptions(command.courseId, command.lessonNumber)
        if (command.questionIds.isEmpty()) return questions
        val wanted = command.questionIds.toSet()
        val picked = questions.filter { it.id in wanted }
        assertCheck(picked.size == wanted.size) { ErrorCode.NOT_FOUND }
        return picked
    }

    /** 문제를 더할 때와 같은 조건이다. 연 차시에 문제가 늘면 먼저 푼 학생과 점수의 분모가 달라진다. */
    private fun validateTarget(courseId: Long, lessonNumber: Int) {
        lessonRepository.getByNumber(courseId, lessonNumber)
        val locked =
            lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId, lessonNumber) ||
                lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(
                    courseId,
                    lessonNumber,
                ) ||
                attemptRepository.existsByClassroomCourseIdAndLessonNumber(courseId, lessonNumber)
        assertCheck(!locked) { ErrorCode.LESSON_LOCKED_BY_OPENING }
    }

    private fun createLesson(command: QuestionCommand.Copy, title: String): Int {
        val course = courseRepository.getOwned(command.targetCourseId, command.userId)
        val lessonNumber = (lessonRepository.findMaxLessonNumber(command.targetCourseId) ?: 0) + 1
        lessonRepository.save(
            Lesson.create(course, lessonNumber, LessonCommand.Create(command.userId, command.targetCourseId, title)),
        )
        return lessonNumber
    }

    private fun toCreate(question: Question, command: QuestionCommand.Copy, lessonNumber: Int) = QuestionCommand.Create(
        userId = command.userId,
        courseId = command.targetCourseId,
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
}
