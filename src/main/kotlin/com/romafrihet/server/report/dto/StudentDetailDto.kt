package com.romafrihet.server.report.dto

import com.romafrihet.server.lesson.QuestionResult

/** 학생 한 명의 차시별 점수와 최근 차시의 문제별 결과다. */
data class StudentDetailDto(
    val membershipId: Long,
    val classroomId: Long,
    val classNo: Int,
    val studentName: String,
    val studentNumber: String,
    val scores: List<LessonScoreDto>,
    val totalCorrectCount: Int,
    val totalQuestionCount: Int,
    val latestLessonNumber: Int?,
    val latestAnswers: List<StudentAnswerDto>,
)

/** 교사가 결과를 고치려면 attemptAnswerId가 필요하다. 시작하지 않은 문제는 행이 없어 담기지 않는다. */
data class StudentAnswerDto(
    val attemptAnswerId: Long,
    val questionIndex: Int,
    val questionId: Long,
    val content: String?,
    val result: QuestionResult,
    val isOverridden: Boolean,
    val byTimeout: Boolean,
)
