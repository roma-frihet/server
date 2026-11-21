package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.QuestionCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QuestionEditFlowTest : TeacherFlowFixture() {
    @Test
    fun `제한 시간을 주지 않으면 30초로 두고 해설은 비워 둔다`() {
        val questionId = createQuestion()

        val question = findQuestion(questionId)

        assertThat(question.timeLimitSec).isEqualTo(30)
        assertThat(question.explanation).isNull()
    }

    @Test
    fun `문제는 차시 안에서 추가한 순서대로 번호가 붙는다`() {
        val first = createQuestion(lessonNumber = 1)
        val second = createQuestion(lessonNumber = 1)

        val orders = questionService
            .findAll(QuestionCommand.Query(teacherId, courseId, 1))
            .map { it.id to it.displayOrder }

        assertThat(orders).containsExactly(first to 0, second to 1)
    }

    @Test
    fun `가운데 문제를 지운 뒤에도 문제를 더 넣을 수 있다`() {
        createQuestion(lessonNumber = 1)
        val middle = createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1)

        // 0,1,2에서 1을 지우면 0,2가 남는다. 개수로 다음 번호를 정하면 2를 또 주게 된다.
        questionService.delete(QuestionCommand.Delete(teacherId, middle))
        val added = createQuestion(lessonNumber = 1)

        val orders = questionService
            .findAll(QuestionCommand.Query(teacherId, courseId, 1))
            .map { it.displayOrder }

        assertThat(orders).doesNotHaveDuplicates()
        assertThat(findQuestion(added).displayOrder).isEqualTo(3)
    }

    @Test
    fun `아무도 풀지 않았으면 차시가 열려 있어도 문제를 고칠 수 있다`() {
        val classroomId = createClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        questionService.update(updateCommand(questionId, content = "고친 문제", timeLimitSec = 60))

        val question = findQuestion(questionId)
        assertThat(question.content).isEqualTo("고친 문제")
        assertThat(question.timeLimitSec).isEqualTo(60)
    }

    @Test
    fun `제출이 있으면 글과 해설은 고칠 수 있다`() {
        val questionId = submittedQuestion()

        questionService.update(updateCommand(questionId, content = "오타를 고친 문제", explanation = "해설을 붙였다"))

        val question = findQuestion(questionId)
        assertThat(question.content).isEqualTo("오타를 고친 문제")
        assertThat(question.explanation).isEqualTo("해설을 붙였다")
    }

    @Test
    fun `제출이 있으면 제한 시간은 고칠 수 없다`() {
        val questionId = submittedQuestion()

        assertErrorCode(ErrorCode.QUESTION_LOCKED_BY_SUBMISSION) {
            questionService.update(updateCommand(questionId, timeLimitSec = 120))
        }
    }

    @Test
    fun `제출이 있으면 정답을 바꿀 수 없다`() {
        val questionId = submittedQuestion()

        assertErrorCode(ErrorCode.QUESTION_LOCKED_BY_SUBMISSION) {
            questionService.update(
                updateCommand(
                    questionId,
                    options = listOf(choice("엽록체"), choice("미토콘드리아", correct = true)),
                ),
            )
        }
    }

    @Test
    fun `연 적 있는 차시에는 문제를 더할 수 없다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) { createQuestion(lessonNumber = 1) }
    }

    private fun submittedQuestion(): Long {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)
        return questionId
    }

    private fun findQuestion(questionId: Long) = questionService.find(QuestionCommand.Get(teacherId, questionId))

    private fun updateCommand(
        questionId: Long,
        content: String = "광합성이 일어나는 곳은?",
        timeLimitSec: Int = 30,
        explanation: String? = null,
        options: List<QuestionCommand.Option> =
            listOf(choice("엽록체", correct = true), choice("미토콘드리아")),
    ) = QuestionCommand.Update(
        userId = teacherId,
        questionId = questionId,
        type = QuestionType.MULTIPLE_CHOICE,
        content = content,
        imageId = null,
        timeLimitSec = timeLimitSec,
        explanation = explanation,
        options = options,
    )
}
