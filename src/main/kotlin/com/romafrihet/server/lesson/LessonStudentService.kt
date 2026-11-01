package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.dto.OpenedLessonDto
import com.romafrihet.server.lesson.dto.QuestionDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LessonStudentService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
    private val questionImageUrlResolver: QuestionImageUrlResolver,
    private val lessonRepository: LessonRepository,
) {
    /** 닫은 차시는 학생에게 보이지 않는다. */
    fun findOpenedLessons(userId: UUID, classroomId: Long): List<OpenedLessonDto> {
        validateApproved(userId, classroomId)
        val classroom = getClassroom(classroomId)
        val titles =
            lessonRepository
                .findAllByCourseIdOrderByLessonNumberAsc(requireNotNull(classroom.course.id))
                .associate { it.lessonNumber to it.title }
        return lessonOpeningRepository
            .findAllByClassroomIdOrderByLessonNumberAsc(classroomId)
            .filter { it.status != LessonStatus.CLOSED }
            .map {
                OpenedLessonDto(
                    lessonNumber = it.lessonNumber,
                    title = titles[it.lessonNumber].orEmpty(),
                    openedAt = it.openedAt,
                    status = it.status,
                )
            }
    }

    /** 지금 열린 차시의 문제만 내린다. 서명 URL 발급 때문에 트랜잭션 없이 실행한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun findQuestions(command: QuestionCommand.StudentQuery): List<QuestionDto> {
        validateApproved(command.userId, command.classroomId)
        val opening =
            lessonOpeningRepository.findByClassroomIdAndLessonNumber(command.classroomId, command.lessonNumber)
        if (opening?.isOpen != true) throw BusinessException(ErrorCode.NOT_FOUND)
        val classroom = getClassroom(command.classroomId)
        return questionImageUrlResolver.toDtos(
            questionRepository.findAllWithOptions(requireNotNull(classroom.course.id), command.lessonNumber),
        )
    }

    private fun getClassroom(classroomId: Long) =
        classroomRepository.findById(classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }

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
