package com.romafrihet.server.report.dto

import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.QuestionResult
import java.time.LocalDateTime

/**
 * 학생이 보는 자기 리포트다. 다른 학생의 이름이나 점수는 담지 않는다. 반 평균만 준다.
 * 피드백은 나중에 여기에 필드로 붙인다.
 */
data class StudentReportDto(
    val classroomId: Long,
    val classNo: Int,
    val grade: Int,
    val subjectName: String,
    val openLesson: OpenLessonDto?,
    val latestScore: LessonScoreDto?,
    val scores: List<LessonScoreDto>,
    val totalCorrectCount: Int,
    val totalQuestionCount: Int,
    val recentWrongQuestions: List<WrongQuestionDto>,
)

data class OpenLessonDto(
    val lessonNumber: Int,
    val title: String,
    val questionCount: Int,
    val totalTimeLimitSec: Int,
    val openedAt: LocalDateTime,
    val phase: AttemptPhase,
    val nextIndex: Int,
)

/** 마감한 차시에서 틀리거나 답하지 못한 문제다. 마감 전에는 정답을 공개하지 않으므로 담지 않는다. */
data class WrongQuestionDto(
    val lessonNumber: Int,
    val lessonTitle: String,
    val questionIndex: Int,
    val questionId: Long,
    val content: String?,
    val result: QuestionResult,
    val correctAnswer: String?,
    val explanation: String?,
)
