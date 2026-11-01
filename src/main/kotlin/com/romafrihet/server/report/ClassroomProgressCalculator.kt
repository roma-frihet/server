package com.romafrihet.server.report

import com.romafrihet.server.classroom.ClassroomMembership
import com.romafrihet.server.lesson.LessonOpening
import com.romafrihet.server.lesson.LessonSubmission
import com.romafrihet.server.report.dto.ClassroomProgressDto
import com.romafrihet.server.report.dto.LessonScoreDto
import com.romafrihet.server.report.dto.StudentProgressDto
import org.springframework.stereotype.Component

/** 최근 차시는 이 반에 연 차시 중 번호가 가장 큰 차시다. */
@Component
class ClassroomProgressCalculator {
    fun calculate(classroomId: Long, classroomName: String, records: ClassroomRecords): ClassroomProgressDto {
        val students = records.students[classroomId].orEmpty().sortedBy { it.studentNumber }
        val openedLessons = records.openings[classroomId].orEmpty().map(LessonOpening::lessonNumber).sorted()
        val submissions = records.submissions[classroomId].orEmpty()
        val byLesson = submissions.groupBy { it.lessonNumber }
        val byStudent = submissions.groupBy { it.userId }
        val latest = openedLessons.lastOrNull()
        val latestResponses = latest?.let { byLesson[it] }.orEmpty()
        val progress =
            students.map { student ->
                toStudentProgress(
                    student,
                    openedLessons,
                    byStudent[student.userId].orEmpty(),
                    byLesson,
                    records.questionCounts,
                )
            }
        return ClassroomProgressDto(
            classroomId = classroomId,
            classroomName = classroomName,
            studentCount = students.size,
            latestLessonNumber = latest,
            latestQuestionCount = latest?.let { records.questionCounts[it] } ?: 0,
            latestAverageCorrect = averageCorrect(latestResponses),
            latestResponseCount = latestResponses.size,
            watchStudentCount = progress.count { it.status != StudentProgressStatus.NORMAL },
            students = progress,
        )
    }

    fun averageCorrect(submissions: List<LessonSubmission>): Double? =
        submissions.takeIf { it.isNotEmpty() }?.map(LessonSubmission::correctCount)?.average()

    private fun toStudentProgress(
        student: ClassroomMembership,
        openedLessons: List<Int>,
        submissions: List<LessonSubmission>,
        byLesson: Map<Int, List<LessonSubmission>>,
        questionCounts: Map<Int, Int>,
    ): StudentProgressDto {
        val scores = submissions.associateBy { it.lessonNumber }
        val latest = openedLessons.lastOrNull()?.let(scores::get)
        val previous = openedLessons.dropLast(1).lastOrNull()?.let(scores::get)
        return StudentProgressDto(
            membershipId = requireNotNull(student.id),
            studentName = student.studentName,
            studentNumber = student.studentNumber,
            recentScores =
            openedLessons.takeLast(RECENT_LESSON_COUNT).map { lessonNumber ->
                LessonScoreDto(
                    lessonNumber = lessonNumber,
                    correctCount = scores[lessonNumber]?.correctCount,
                    questionCount = questionCounts[lessonNumber] ?: 0,
                    classAverageCorrect = averageCorrect(byLesson[lessonNumber].orEmpty()),
                )
            },
            latestCorrectCount = latest?.correctCount,
            previousCorrectCount = previous?.correctCount,
            totalCorrectCount = submissions.sumOf { it.correctCount },
            totalQuestionCount = submissions.sumOf { it.questionCount },
            status = statusOf(openedLessons, latest, previous),
        )
    }

    private fun statusOf(
        openedLessons: List<Int>,
        latest: LessonSubmission?,
        previous: LessonSubmission?,
    ): StudentProgressStatus = when {
        openedLessons.isNotEmpty() && latest == null -> StudentProgressStatus.NOT_RESPONDED
        latest != null && previous != null && rateOf(previous) - rateOf(latest) >= DROP_RATE ->
            StudentProgressStatus.SCORE_DROPPED
        else -> StudentProgressStatus.NORMAL
    }

    private fun rateOf(submission: LessonSubmission): Double =
        if (submission.questionCount == 0) 0.0 else submission.correctCount.toDouble() / submission.questionCount

    private companion object {
        const val RECENT_LESSON_COUNT = 6
        const val DROP_RATE = 0.2
    }
}
