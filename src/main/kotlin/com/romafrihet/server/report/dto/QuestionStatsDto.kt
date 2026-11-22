package com.romafrihet.server.report.dto

import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionType

/** 한 반에서 한 차시의 문제별 결과다. 반 비교는 이 값을 반마다 나란히 놓는다. */
data class ClassroomQuestionStatsDto(
    val classroomId: Long,
    val classNo: Int,
    val status: LessonStatus,
    val studentCount: Int,
    val responseCount: Int,
    val questions: List<QuestionStatDto>,
)

data class QuestionStatDto(
    val questionId: Long,
    val displayOrder: Int,
    val type: QuestionType,
    val content: String?,
    val hasImage: Boolean,
    val answeredCount: Int,
    val rightCount: Int,
    val noAnswerCount: Int,
    val options: List<OptionStatDto>,
    val acceptedAnswers: List<String>,
    val answerTexts: List<AnswerTextStatDto>,
)

data class OptionStatDto(
    val optionId: Long,
    val displayOrder: Int,
    val content: String?,
    val hasImage: Boolean,
    val isCorrect: Boolean,
    val count: Int,
)

/** 정규화한 값이 같은 주관식 답을 묶었다. answerText는 그중 가장 많이 쓴 원문이다. */
data class AnswerTextStatDto(val answerText: String, val count: Int, val rightCount: Int)
