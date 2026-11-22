package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class QuestionCopyFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var questionCopyService: QuestionCopyService

    @Test
    fun `대상 차시를 비우면 원본 제목으로 새 차시를 만들어 모든 문제를 복사한다`() {
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1)

        val result = questionCopyService.copy(copy(targetCourseId = courseId))

        assertThat(result.targetLessonNumber).isEqualTo(LESSON_COUNT + 1)
        assertThat(result.copiedCount).isEqualTo(2)
        val lesson = lessonService.findAll(LessonCommand.Query(teacherId, courseId)).last()
        assertThat(lesson.title).isEqualTo("1차시 내용")
        val copied = questions(courseId, result.targetLessonNumber)
        assertThat(copied.map { it.displayOrder }).containsExactly(0, 1)
        assertThat(copied.first().options.map { it.content to it.isCorrect })
            .containsExactly("엽록체" to true, "미토콘드리아" to false)
    }

    @Test
    fun `고른 문제만 다른 수업의 기존 차시 맨 뒤에 붙인다`() {
        createQuestion(lessonNumber = 1)
        val second = createQuestion(lessonNumber = 1)
        val otherCourse = createCourse("수학")
        addLesson(otherCourse, "수학 1차시")
        createQuestion(lessonNumber = 1, courseId = otherCourse)

        val result =
            questionCopyService.copy(
                copy(targetCourseId = otherCourse, targetLessonNumber = 1, questionIds = listOf(second)),
            )

        assertThat(result.copiedCount).isEqualTo(1)
        assertThat(questions(otherCourse, 1).map { it.displayOrder }).containsExactly(0, 1)
    }

    @Test
    fun `복사본을 고쳐도 원본은 그대로이고 원본을 지워도 함께 쓰는 이미지는 남는다`() {
        val image = saveImage(teacherId)
        val original =
            questionService.create(
                QuestionCommand.Create(
                    userId = teacherId,
                    courseId = courseId,
                    lessonNumber = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    content = null,
                    imageId = image.id,
                    timeLimitSec = 30,
                    explanation = "해설",
                    options = listOf(choice("가", correct = true), choice("나")),
                ),
            )
        val result = questionCopyService.copy(copy(targetCourseId = courseId))
        val copiedId = requireNotNull(questions(courseId, result.targetLessonNumber).single().id)

        questionService.update(
            QuestionCommand.Update(
                userId = teacherId,
                questionId = copiedId,
                type = QuestionType.MULTIPLE_CHOICE,
                content = "바꾼 문제",
                imageId = image.id,
                timeLimitSec = 60,
                explanation = null,
                options = listOf(choice("다", correct = true), choice("라")),
            ),
        )
        questionService.delete(QuestionCommand.Delete(teacherId, original))

        assertThat(imageRepository.findById(requireNotNull(image.id))).isPresent()
        assertThat(questions(courseId, 1)).isEmpty()
        assertThat(questions(courseId, result.targetLessonNumber).single().content).isEqualTo("바꾼 문제")
    }

    @Test
    fun `연 차시에는 넣지 못하고 다른 차시의 문제나 다른 교사의 수업은 404다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        val otherLessonQuestion = createQuestion(lessonNumber = 2)
        openLesson(classroomId, 2)

        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) {
            questionCopyService.copy(copy(targetCourseId = courseId, targetLessonNumber = 2))
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            questionCopyService.copy(copy(targetCourseId = courseId, questionIds = listOf(otherLessonQuestion)))
        }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            questionCopyService.copy(copy(targetCourseId = courseId).copy(userId = UUID.randomUUID()))
        }
    }

    private fun copy(targetCourseId: Long, targetLessonNumber: Int? = null, questionIds: List<Long> = emptyList()) =
        QuestionCommand.Copy(teacherId, courseId, 1, targetCourseId, targetLessonNumber, questionIds)

    private fun questions(courseId: Long, lessonNumber: Int) =
        questionRepository.findAllWithOptions(courseId, lessonNumber)
}
