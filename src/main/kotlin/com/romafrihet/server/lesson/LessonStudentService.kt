package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.dto.OpenedLessonDto
import org.springframework.stereotype.Service
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
    private val lessonScheduleApplier: LessonScheduleApplier,
) {
    /**
     * 닫은 차시는 학생에게 보이지 않는다.
     * 열기 예약은 학생이 이 목록을 읽을 때 반영되는 경우가 많으므로, 읽기 전에 적용한다. 그래서 readOnly가 아니다.
     */
    @Transactional
    fun findOpenedLessons(userId: UUID, classroomId: Long): List<OpenedLessonDto> {
        validateApproved(userId, classroomId)
        lessonScheduleApplier.applyDue(listOf(classroomId))
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

    private fun getClassroom(classroomId: Long) =
        classroomRepository.findById(classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }

    private fun validateApproved(userId: UUID, classroomId: Long) {
        assertCheck(
            classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, userId),
        ) { ErrorCode.FORBIDDEN }
    }
}
