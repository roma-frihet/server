package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.SubmissionCommand
import com.romafrihet.server.lesson.dto.SubmissionQuestionDto
import com.romafrihet.server.lesson.dto.SubmissionResultDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LessonSubmissionService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val lessonSubmissionRepository: LessonSubmissionRepository,
    private val questionRepository: QuestionRepository,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
) {
    /** 제출 응답에 점수를 담지 않는다. 열려 있는 동안 정답을 알아낼 수 없게 하기 위해서다. */
    @Transactional
    fun submit(command: SubmissionCommand.Create): Long {
        validateApproved(command.userId, command.classroomId)
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(command.classroomId, command.lessonNumber)
        assertCheck(opening != null && opening.isOpen) { ErrorCode.LESSON_NOT_OPEN }
        assertCheck(
            !lessonSubmissionRepository.existsByClassroomIdAndLessonNumberAndUserId(
                command.classroomId,
                command.lessonNumber,
                command.userId,
            ),
        ) { ErrorCode.SUBMISSION_ALREADY_EXISTS }

        val classroom =
            classroomRepository.findById(command.classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
        val questions =
            questionRepository.findAllWithOptions(requireNotNull(classroom.course.id), command.lessonNumber)
        val answers = toAnswerMap(command.answers, questions)
        val submission =
            LessonSubmission.create(classroom, command.lessonNumber, command.userId, questions.size)
        questions.forEach { question ->
            val answer = answers[question.id]
            submission.addAnswer(question, answer?.optionId, answer?.text)
        }
        return requireNotNull(lessonSubmissionRepository.save(submission).id)
    }

    /** 마감한 차시만 정답을 공개한다. 서명 URL 발급 때문에 트랜잭션 없이 실행한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun findResult(command: SubmissionCommand.Query): SubmissionResultDto {
        validateApproved(command.userId, command.classroomId)
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(command.classroomId, command.lessonNumber)
        if (opening?.status != LessonStatus.FINISHED) throw BusinessException(ErrorCode.NOT_FOUND)
        val classroom =
            classroomRepository.findById(command.classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
        val questions =
            questionImageUrlResolver.toDtos(
                questionRepository.findAllWithOptions(requireNotNull(classroom.course.id), command.lessonNumber),
            )
        val submission =
            lessonSubmissionRepository.findWithAnswers(command.classroomId, command.lessonNumber, command.userId)
        val answers = submission?.answers.orEmpty().associateBy { it.question.id }
        return SubmissionResultDto(
            lessonNumber = command.lessonNumber,
            correctCount = submission?.correctCount,
            questionCount = questions.size,
            questions =
            questions.map { question ->
                val answer = answers[question.id]
                SubmissionQuestionDto(
                    question = question,
                    selectedOptionId = answer?.optionId,
                    answerText = answer?.answerText,
                    isCorrect = answer?.isCorrect ?: false,
                )
            },
        )
    }

    private fun toAnswerMap(
        answers: List<SubmissionCommand.Answer>,
        questions: List<Question>,
    ): Map<Long, SubmissionCommand.Answer> {
        val byQuestion = answers.associateBy { it.questionId }
        val questionsById = questions.associateBy { requireNotNull(it.id) }
        assertCheck(byQuestion.size == answers.size) { ErrorCode.SUBMISSION_ANSWER_INVALID }
        assertCheck(byQuestion.all { (id, answer) -> questionsById[id]?.accepts(answer.optionId) == true }) {
            ErrorCode.SUBMISSION_ANSWER_INVALID
        }
        return byQuestion
    }

    private fun validateApproved(userId: UUID, classroomId: Long) {
        assertCheck(
            classroomMembershipRepository.existsByClassroomIdAndUserIdAndStatus(
                classroomId,
                userId,
                MembershipStatus.APPROVED,
            ),
        ) { ErrorCode.FORBIDDEN }
    }
}
