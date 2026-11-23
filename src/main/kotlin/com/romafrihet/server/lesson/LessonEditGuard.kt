package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import org.springframework.stereotype.Component

/**
 * 차시에 문제를 더하거나 내용을 바꿀 수 있는지다. 이미 연 차시에 문제가 늘면 점수의 분모가 달라져
 * 먼저 푼 학생과 나중에 푼 학생을 비교할 수 없게 된다. 문제 추가와 복사가 같은 기준을 쓰도록 한 곳에 둔다.
 */
@Component
class LessonEditGuard(
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val attemptRepository: AttemptRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonScheduleApplier: LessonScheduleApplier,
) {
    /**
     * 열기 시각이 지났지만 아직 적용되지 않은 예약을 먼저 적용한다. 그러지 않으면 열려야 할 차시가
     * 열지 않은 차시로 보여 문제가 늘어난다. 쓰기이므로 호출 측 트랜잭션은 readOnly가 아니어야 한다.
     */
    fun validateCanAddQuestions(courseId: Long, lessonNumber: Int) {
        lessonScheduleApplier.applyDue(
            classroomRepository.findAllByCourseIdOrderByClassNoAsc(courseId).map { requireNotNull(it.id) },
        )
        val locked =
            lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndClosedAtIsNull(courseId, lessonNumber) ||
                lessonOpeningRepository.existsByClassroomCourseIdAndLessonNumberAndFinishedAtIsNotNull(
                    courseId,
                    lessonNumber,
                ) ||
                attemptRepository.existsByClassroomCourseIdAndLessonNumber(courseId, lessonNumber)
        assertCheck(!locked) { ErrorCode.LESSON_LOCKED_BY_OPENING }
    }
}
