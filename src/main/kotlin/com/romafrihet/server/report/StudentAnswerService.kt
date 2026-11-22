package com.romafrihet.server.report

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.AttemptAnswer
import com.romafrihet.server.lesson.AttemptAnswerRepository
import com.romafrihet.server.lesson.AttemptFinalizer
import com.romafrihet.server.lesson.AttemptRepository
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.Question
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.getByNumber
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.AnswerOptionDto
import com.romafrihet.server.report.dto.StudentLessonAnswerDto
import com.romafrihet.server.report.dto.StudentLessonAnswersDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 학생 한 명이 한 차시에서 문제마다 무엇을 골랐고 썼는지다. 시작하지 않은 문제도 순서대로 넣는다. */
@Service
@Transactional(readOnly = true)
class StudentAnswerService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonRepository: LessonRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val attemptFinalizer: AttemptFinalizer,
) {
    /** 읽기 전에 시간이 지난 응답을 확정하므로 readOnly가 아니다. */
    @Transactional
    fun findLessonAnswers(command: ReportCommand.StudentLessonAnswers): StudentLessonAnswersDto {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val courseId = requireNotNull(classroom.course.id)
        val membership =
            classroomMembershipRepository.findByIdAndClassroomId(command.membershipId, command.classroomId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND)
        val lesson = lessonRepository.getByNumber(courseId, command.lessonNumber)
        val questions = questionRepository.findAllWithOptions(courseId, command.lessonNumber)
        val attempt =
            attemptRepository.findByClassroomIdAndLessonNumberAndUserId(
                command.classroomId,
                command.lessonNumber,
                membership.userId,
            )
        val answers =
            attempt
                ?.also { attemptFinalizer.finalizeExpired(listOf(it), mapOf(command.lessonNumber to questions.size)) }
                ?.id
                ?.let(attemptAnswerRepository::findAllByAttemptIdOrderByQuestionIndexAsc)
                .orEmpty()
                .associateBy { requireNotNull(it.question.id) }
        val rows = questions.map { toAnswer(it, answers[it.id]) }
        return StudentLessonAnswersDto(
            membershipId = requireNotNull(membership.id),
            studentName = membership.studentName,
            studentNumber = membership.studentNumber,
            lessonNumber = command.lessonNumber,
            lessonTitle = lesson.title,
            status =
            lessonOpeningRepository
                .findByClassroomIdAndLessonNumber(command.classroomId, command.lessonNumber)
                ?.status
                ?: LessonStatus.PLANNED,
            questionCount = questions.size,
            correctCount = rows.count { it.result == StudentAnswerResult.RIGHT },
            answers = rows,
        )
    }

    private fun toAnswer(question: Question, answer: AttemptAnswer?): StudentLessonAnswerDto {
        val options = question.options.sortedBy { it.displayOrder }
        val isChoice = question.type == QuestionType.MULTIPLE_CHOICE
        return StudentLessonAnswerDto(
            questionId = requireNotNull(question.id),
            displayOrder = question.displayOrder,
            type = question.type,
            content = question.content,
            hasImage = question.imageId != null,
            explanation = question.explanation,
            options =
            if (isChoice) {
                options.map {
                    AnswerOptionDto(
                        optionId = requireNotNull(it.id),
                        displayOrder = it.displayOrder,
                        content = it.content,
                        hasImage = it.imageId != null,
                        isCorrect = it.isCorrect,
                    )
                }
            } else {
                emptyList()
            },
            acceptedAnswers = if (isChoice) emptyList() else options.mapNotNull { it.content },
            selectedOptionId = answer?.optionId,
            answerText = answer?.answerText,
            result = resultOf(answer),
            attemptAnswerId = answer?.id,
            isOverridden = answer?.isOverridden ?: false,
            byTimeout = answer?.byTimeout ?: false,
        )
    }

    private fun resultOf(answer: AttemptAnswer?): StudentAnswerResult = when {
        answer == null -> StudentAnswerResult.NOT_STARTED
        !answer.isFinalized -> StudentAnswerResult.IN_PROGRESS
        else ->
            when (answer.finalResult) {
                QuestionResult.RIGHT -> StudentAnswerResult.RIGHT
                QuestionResult.WRONG -> StudentAnswerResult.WRONG
                QuestionResult.NONE -> StudentAnswerResult.NONE
            }
    }
}
