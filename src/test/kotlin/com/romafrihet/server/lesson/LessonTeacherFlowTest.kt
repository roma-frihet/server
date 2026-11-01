package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class LessonTeacherFlowTest : TeacherFlowFixture() {
    @Test
    fun `닫은 차시는 다시 열고 마감한 차시는 다시 열 수 없다`() {
        val classroomId = createClassroom()
        createQuestion(lessonNumber = 1)
        val change = LessonOpeningCommand.Change(teacherId, classroomId, 1)
        openingService.open(change)
        openingService.close(change)
        openingService.open(change)

        openingService.finish(change)

        assertErrorCode(ErrorCode.LESSON_FINISHED) { openingService.open(change) }
        assertErrorCode(ErrorCode.LESSON_FINISHED) { openingService.finish(change) }
        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) { createQuestion(lessonNumber = 1) }
    }

    @Test
    fun `차시는 추가한 순서로 번호가 붙고 이름을 고칠 수 있다`() {
        val id = createCourse("영어")
        assertThat(addLesson(id, "첫 차시").lessonNumber).isEqualTo(1)
        assertThat(addLesson(id, "둘째 차시").lessonNumber).isEqualTo(2)

        lessonService.rename(LessonCommand.Rename(teacherId, id, 2, "바뀐 이름"))

        assertThat(lessonService.findAll(LessonCommand.Query(teacherId, id)).map { it.title })
            .containsExactly("첫 차시", "바뀐 이름")
    }

    @Test
    fun `문제는 수업마다 따로 관리하고 다른 수업의 차시를 잠그지 않는다`() {
        val classroomId = approvedClassroom()
        val otherCourseId = createCourse("수학")
        addLesson(otherCourseId, "다른 수업 1차시")
        createQuestion(lessonNumber = 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        createQuestion(lessonNumber = 1, courseId = otherCourseId)

        assertThat(questionService.findAll(QuestionCommand.Query(teacherId, otherCourseId, 1))).hasSize(1)
        assertErrorCode(ErrorCode.NOT_FOUND) {
            questionService.findAll(QuestionCommand.Query(UUID.randomUUID(), otherCourseId, 1))
        }
    }

    @Test
    fun `현재 차시는 열린 차시이고 닫으면 다음 차시가 된다`() {
        val classroomId = approvedClassroom()
        createQuestion(lessonNumber = 3)

        assertThat(openingService.findStatuses(teacherId).single().currentLessonNumber).isEqualTo(1)

        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 3))
        assertThat(openingService.findStatuses(teacherId).single().currentLessonNumber).isEqualTo(3)

        openingService.close(LessonOpeningCommand.Change(teacherId, classroomId, 3))
        val closed = openingService.findStatuses(teacherId).single()
        assertThat(closed.currentLessonNumber).isEqualTo(4)
        assertThat(closed.latestLessonNumber).isEqualTo(3)
        assertThat(closed.isOpen).isFalse()
    }

    @Test
    fun `반에 열린 차시가 있으면 다른 차시를 열 수 없고 닫은 차시는 다시 열 수 있다`() {
        val classroomId = approvedClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertErrorCode(ErrorCode.LESSON_ALREADY_OPEN) {
            openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 2))
        }

        openingService.close(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertThat(openingService.findStatuses(teacherId).single().isOpen).isTrue()
    }

    @Test
    fun `문제가 없는 차시는 열 수 없다`() {
        val classroomId = approvedClassroom()

        assertErrorCode(ErrorCode.LESSON_HAS_NO_QUESTIONS) {
            openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        }
    }

    @Test
    fun `열린 차시의 문제는 추가 수정 삭제할 수 없다`() {
        val classroomId = approvedClassroom()
        val questionId = createQuestion()
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 3))

        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) { createQuestion() }
        assertErrorCode(ErrorCode.LESSON_LOCKED_BY_OPENING) {
            questionService.delete(QuestionCommand.Delete(teacherId, questionId))
        }
    }

    @Test
    fun `수정하면 보기 수가 바뀌고 빠진 이미지만 지운다`() {
        val kept = saveImage(teacherId)
        val removed = saveImage(teacherId)
        val questionId =
            questionService.create(
                QuestionCommand.Create(
                    userId = teacherId,
                    courseId = courseId,
                    lessonNumber = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    content = "그림을 고르시오",
                    imageId = null,
                    options =
                    listOf(
                        choice(null, correct = true, imageId = kept.id),
                        choice(null, imageId = removed.id),
                        choice("셋째"),
                    ),
                ),
            )

        questionService.update(
            QuestionCommand.Update(
                userId = teacherId,
                questionId = questionId,
                type = QuestionType.MULTIPLE_CHOICE,
                content = "그림을 고르시오",
                imageId = null,
                options = listOf(choice("첫째"), choice(null, correct = true, imageId = kept.id)),
            ),
        )

        val updated = questionService.find(QuestionCommand.Get(teacherId, questionId))
        assertThat(updated.options.map { it.imageId }).containsExactly(null, kept.id)
        verify(exactly = 1) { storageClient.delete(listOf(removed.storagePath)) }
        assertThat(imageRepository.existsById(requireNotNull(removed.id))).isFalse()
        assertThat(imageRepository.existsById(requireNotNull(kept.id))).isTrue()
    }

    @Test
    fun `다른 문제가 쓰는 이미지는 문제를 지워도 남긴다`() {
        val shared = saveImage(teacherId)
        val create = QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = 1,
            type = QuestionType.MULTIPLE_CHOICE,
            content = null,
            imageId = shared.id,
            options = listOf(choice("가", correct = true), choice("나")),
        )
        val first = questionService.create(create)
        val second = questionService.create(create)

        questionService.delete(QuestionCommand.Delete(teacherId, first))
        assertThat(imageRepository.existsById(requireNotNull(shared.id))).isTrue()

        questionService.delete(QuestionCommand.Delete(teacherId, second))
        assertThat(imageRepository.existsById(requireNotNull(shared.id))).isFalse()
        verify(exactly = 1) { storageClient.delete(listOf(shared.storagePath)) }
    }

    @Test
    fun `주관식은 인정 답안을 여러 개 저장하고 삭제하면 사라진다`() {
        val questionId =
            questionService.create(
                QuestionCommand.Create(
                    userId = teacherId,
                    courseId = courseId,
                    lessonNumber = 1,
                    type = QuestionType.SHORT_ANSWER,
                    content = "광합성에 필요한 기체를 쓰시오.",
                    imageId = null,
                    options = listOf(choice("이산화탄소", correct = true), choice("CO2", correct = true)),
                ),
            )
        assertThat(questionService.find(QuestionCommand.Get(teacherId, questionId)).options).hasSize(2)

        questionService.delete(QuestionCommand.Delete(teacherId, questionId))

        assertErrorCode(ErrorCode.NOT_FOUND) { questionService.find(QuestionCommand.Get(teacherId, questionId)) }
    }

    @Test
    fun `다른 교사의 문제와 이미지는 쓸 수 없다`() {
        val questionId = createQuestion()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            questionService.delete(QuestionCommand.Delete(UUID.randomUUID(), questionId))
        }
        assertErrorCode(ErrorCode.QUESTION_IMAGE_NOT_USABLE) {
            questionService.create(
                QuestionCommand.Create(
                    userId = teacherId,
                    courseId = courseId,
                    lessonNumber = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    content = null,
                    imageId = saveImage(UUID.randomUUID()).id,
                    options = listOf(choice("가", correct = true), choice("나")),
                ),
            )
        }
    }

    @Test
    fun `객관식 정답이 2개면 거절한다`() {
        assertErrorCode(ErrorCode.QUESTION_OPTIONS_INVALID) {
            questionService.create(
                QuestionCommand.Create(
                    userId = teacherId,
                    courseId = courseId,
                    lessonNumber = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    content = "문제",
                    imageId = null,
                    options = listOf(choice("가", correct = true), choice("나", correct = true)),
                ),
            )
        }
    }
}
