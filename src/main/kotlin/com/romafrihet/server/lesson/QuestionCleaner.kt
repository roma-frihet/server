package com.romafrihet.server.lesson

import com.romafrihet.server.lesson.command.QuestionCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 수업이나 차시를 통째로 지울 때 그 안의 문제를 치운다.
 *
 * 하나씩 지우는 [QuestionService.delete]와 달리 막지 않는다. 푼 기록은 부르는 쪽이 먼저
 * 지웠거나(수업·반 삭제) 없다는 것을 확인한 뒤(차시 삭제)이기 때문이다.
 */
@Service
@Transactional(readOnly = true)
class QuestionCleaner(
    private val questionRepository: QuestionRepository,
    private val questionImageCleaner: QuestionImageCleaner,
) {
    @Transactional
    fun deleteAllInCourse(command: QuestionCommand.DeleteAllInCourse) {
        deleteAll(questionRepository.findAllWithOptionsByCourseId(command.courseId))
    }

    @Transactional
    fun deleteAllInLesson(command: QuestionCommand.DeleteAllInLesson) {
        deleteAll(questionRepository.findAllWithOptions(command.courseId, command.lessonNumber))
    }

    /** 보기는 cascade와 orphanRemoval로 함께 지워지므로 문제 엔티티만 넘긴다. */
    private fun deleteAll(questions: List<Question>) {
        if (questions.isEmpty()) return
        val images = questions.flatMap { it.imageIds() }
        questionRepository.deleteAll(questions)
        questionImageCleaner.removeUnused(images)
    }
}
