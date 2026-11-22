package com.romafrihet.server.lesson

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
) {
    fun validateCanAddQuestions(courseId: Long, lessonNumber: Int) {
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
