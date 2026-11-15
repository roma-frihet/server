package com.romafrihet.server.report

import com.romafrihet.server.classroom.ClassroomMembership
import com.romafrihet.server.lesson.LessonOpening
import com.romafrihet.server.lesson.projection.AttemptScoreProjection
import com.romafrihet.server.report.dto.ClassroomProgressDto
import com.romafrihet.server.report.dto.LessonScoreDto
import com.romafrihet.server.report.dto.StudentProgressDto
import org.springframework.stereotype.Component
import java.util.UUID

/** 최근 차시는 이 반에 연 차시 중 번호가 가장 큰 차시다. */
@Component
class ClassroomProgressCalculator {
    fun calculate(classroomId: Long, classNo: Int, records: ClassroomRecords): ClassroomProgressDto {
        val students = records.students[classroomId].orEmpty().sortedBy { it.studentNumber }
        val openedLessons = records.openings[classroomId].orEmpty().map(LessonOpening::lessonNumber).sorted()
        val scores = records.scores[classroomId].orEmpty()
        val latest = openedLessons.lastOrNull()
        val progress =
            students.map { toStudentProgress(it, openedLessons, scores, records.questionCounts) }
        return ClassroomProgressDto(
            classroomId = classroomId,
            classNo = classNo,
            studentCount = students.size,
            latestLessonNumber = latest,
            latestQuestionCount = latest?.let { records.questionCounts[it] } ?: 0,
            latestAverageCorrect = averageCorrect(scores, latest),
            latestResponseCount = latest?.let { lesson -> scores.count { answered(it.value[lesson]) } } ?: 0,
            watchStudentCount = progress.count { it.status != StudentProgressStatus.NORMAL },
            students = progress,
        )
    }

    /** 응답한 학생만 평균에 넣는다. 아무것도 답하지 않은 학생은 0점이 아니라 빠진다. */
    fun averageCorrect(scores: Map<UUID, Map<Int, AttemptScoreProjection>>, lessonNumber: Int?): Double? {
        if (lessonNumber == null) return null
        return scores.values
            .mapNotNull { it[lessonNumber] }
            .filter(::answered)
            .map { it.correctCount.toDouble() }
            .takeIf { it.isNotEmpty() }
            ?.average()
    }

    private fun answered(score: AttemptScoreProjection?): Boolean = (score?.answeredCount ?: 0) > 0

    private fun toStudentProgress(
        student: ClassroomMembership,
        openedLessons: List<Int>,
        scores: Map<UUID, Map<Int, AttemptScoreProjection>>,
        questionCounts: Map<Int, Int>,
    ): StudentProgressDto {
        val mine = scores[student.userId].orEmpty()
        val answeredLessons = openedLessons.filter { answered(mine[it]) }
        val latest = openedLessons.lastOrNull()?.let { mine[it] }?.takeIf(::answered)
        val previous = openedLessons.dropLast(1).lastOrNull()?.let { mine[it] }?.takeIf(::answered)
        return StudentProgressDto(
            membershipId = requireNotNull(student.id),
            studentName = student.studentName,
            studentNumber = student.studentNumber,
            recentScores =
            openedLessons.takeLast(RECENT_LESSON_COUNT).map { lessonNumber ->
                LessonScoreDto(
                    lessonNumber = lessonNumber,
                    correctCount = mine[lessonNumber]?.takeIf(::answered)?.correctCount?.toInt(),
                    questionCount = questionCounts[lessonNumber] ?: 0,
                    classAverageCorrect = averageCorrect(scores, lessonNumber),
                )
            },
            latestCorrectCount = latest?.correctCount?.toInt(),
            previousCorrectCount = previous?.correctCount?.toInt(),
            totalCorrectCount = answeredLessons.sumOf { mine.getValue(it).correctCount.toInt() },
            totalQuestionCount = answeredLessons.sumOf { questionCounts[it] ?: 0 },
            status = statusOf(openedLessons, latest, previous, questionCounts),
        )
    }

    private fun statusOf(
        openedLessons: List<Int>,
        latest: AttemptScoreProjection?,
        previous: AttemptScoreProjection?,
        questionCounts: Map<Int, Int>,
    ): StudentProgressStatus = when {
        openedLessons.isNotEmpty() && latest == null -> StudentProgressStatus.NOT_RESPONDED
        latest != null &&
            previous != null &&
            rateOf(previous, questionCounts) - rateOf(latest, questionCounts) >= DROP_RATE ->
            StudentProgressStatus.SCORE_DROPPED

        else -> StudentProgressStatus.NORMAL
    }

    private fun rateOf(score: AttemptScoreProjection, questionCounts: Map<Int, Int>): Double {
        val questionCount = questionCounts[score.lessonNumber] ?: 0
        return if (questionCount == 0) 0.0 else score.correctCount.toDouble() / questionCount
    }

    private companion object {
        const val RECENT_LESSON_COUNT = 6
        const val DROP_RATE = 0.2
    }
}
