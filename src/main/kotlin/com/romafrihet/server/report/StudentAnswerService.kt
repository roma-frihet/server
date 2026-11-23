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
import com.romafrihet.server.lesson.QuestionImageUrlResolver
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.dto.QuestionDto
import com.romafrihet.server.lesson.getByNumber
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.AnswerOptionDto
import com.romafrihet.server.report.dto.StudentLessonAnswerDto
import com.romafrihet.server.report.dto.StudentLessonAnswersDto
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 학생 한 명이 한 차시에서 문제마다 무엇을 골랐고 썼는지다. 시작하지 않은 문제도 순서대로 넣는다.
 * 이미지만 있는 문제와 보기도 알아볼 수 있게 서명 URL을 붙인다. 서명은 외부 호출이라,
 * 확정과 조회는 트랜잭션 안에서 끝내고 서명은 그 밖에서 한다(AttemptService와 같은 방식).
 */
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
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun findLessonAnswers(command: ReportCommand.StudentLessonAnswers): StudentLessonAnswersDto {
        val loaded = requireNotNull(transactionTemplate.execute { load(command) })
        val questions = questionImageUrlResolver.toDtos(loaded.questions)
        val rows = questions.mapIndexed { index, question ->
            toAnswer(index + 1, question, loaded.answers[question.id])
        }
        return StudentLessonAnswersDto(
            membershipId = loaded.membershipId,
            studentName = loaded.studentName,
            studentNumber = loaded.studentNumber,
            lessonNumber = command.lessonNumber,
            lessonTitle = loaded.lessonTitle,
            status = loaded.status,
            questionCount = questions.size,
            correctCount = rows.count { it.result == StudentAnswerResult.RIGHT },
            answers = rows,
        )
    }

    /** 읽기 전에 시간이 지난 응답을 확정한다. 보기는 fetch join으로 읽어 트랜잭션 밖에서도 쓸 수 있다. */
    private fun load(command: ReportCommand.StudentLessonAnswers): LoadedLesson {
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
                .associate { requireNotNull(it.question.id) to AnswerSnapshot.of(it) }
        return LoadedLesson(
            membershipId = requireNotNull(membership.id),
            studentName = membership.studentName,
            studentNumber = membership.studentNumber,
            lessonTitle = lesson.title,
            status =
            lessonOpeningRepository
                .findByClassroomIdAndLessonNumber(command.classroomId, command.lessonNumber)
                ?.status
                ?: LessonStatus.PLANNED,
            questions = questions,
            answers = answers,
        )
    }

    /** number는 학생이 본 문제 번호다. displayOrder는 가운데 문제를 지우면 빈 번호가 생겨 화면 번호로 쓸 수 없다. */
    private fun toAnswer(number: Int, question: QuestionDto, answer: AnswerSnapshot?): StudentLessonAnswerDto {
        val options = question.options.sortedBy { it.displayOrder }
        val isChoice = question.type == QuestionType.MULTIPLE_CHOICE
        return StudentLessonAnswerDto(
            questionId = question.id,
            number = number,
            displayOrder = question.displayOrder,
            type = question.type,
            content = question.content,
            imageUrl = question.imageUrl,
            explanation = question.explanation,
            options =
            if (isChoice) {
                options.map {
                    AnswerOptionDto(
                        optionId = it.id,
                        displayOrder = it.displayOrder,
                        content = it.content,
                        imageUrl = it.imageUrl,
                        isCorrect = it.isCorrect,
                    )
                }
            } else {
                emptyList()
            },
            acceptedAnswers = if (isChoice) emptyList() else options.mapNotNull { it.content },
            selectedOptionId = answer?.optionId,
            answerText = answer?.answerText,
            result = answer?.result ?: StudentAnswerResult.NOT_STARTED,
            attemptAnswerId = answer?.id,
            isOverridden = answer?.isOverridden ?: false,
            byTimeout = answer?.byTimeout ?: false,
        )
    }

    private class LoadedLesson(
        val membershipId: Long,
        val studentName: String,
        val studentNumber: String,
        val lessonTitle: String,
        val status: LessonStatus,
        val questions: List<Question>,
        val answers: Map<Long, AnswerSnapshot>,
    )

    /** 트랜잭션 밖으로 엔티티를 넘기지 않으려고 필요한 값만 옮겨 담는다. */
    private class AnswerSnapshot(
        val id: Long,
        val optionId: Long?,
        val answerText: String?,
        val result: StudentAnswerResult,
        val isOverridden: Boolean,
        val byTimeout: Boolean,
    ) {
        companion object {
            fun of(answer: AttemptAnswer) = AnswerSnapshot(
                id = requireNotNull(answer.id),
                optionId = answer.optionId,
                answerText = answer.answerText,
                result =
                if (!answer.isFinalized) {
                    StudentAnswerResult.IN_PROGRESS
                } else {
                    when (answer.finalResult) {
                        QuestionResult.RIGHT -> StudentAnswerResult.RIGHT
                        QuestionResult.WRONG -> StudentAnswerResult.WRONG
                        QuestionResult.NONE -> StudentAnswerResult.NONE
                    }
                },
                isOverridden = answer.isOverridden,
                byTimeout = answer.byTimeout,
            )
        }
    }
}
