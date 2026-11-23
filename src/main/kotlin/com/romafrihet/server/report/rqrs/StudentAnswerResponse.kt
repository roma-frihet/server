package com.romafrihet.server.report.rqrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.report.StudentAnswerResult
import com.romafrihet.server.report.dto.AnswerOptionDto
import com.romafrihet.server.report.dto.StudentLessonAnswerDto
import com.romafrihet.server.report.dto.StudentLessonAnswersDto
import io.swagger.v3.oas.annotations.media.Schema

class StudentAnswerResponse private constructor() {
    @Schema(name = "StudentAnswerResponseLesson")
    data class Lesson(
        val membershipId: Long,
        val studentName: String,
        val studentNumber: String,
        val lessonNumber: Int,
        val lessonTitle: String,
        @field:Schema(description = "이 반에서 이 차시의 상태")
        val status: LessonStatus,
        val questionCount: Int,
        val correctCount: Int,
        @field:Schema(description = "문제 순서대로. 시작하지 않은 문제도 들어 있다")
        val answers: List<Answer>,
    ) {
        companion object {
            fun of(dto: StudentLessonAnswersDto) = Lesson(
                membershipId = dto.membershipId,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                lessonNumber = dto.lessonNumber,
                lessonTitle = dto.lessonTitle,
                status = dto.status,
                questionCount = dto.questionCount,
                correctCount = dto.correctCount,
                answers = dto.answers.map(Answer::of),
            )
        }
    }

    @Schema(name = "StudentAnswerResponseAnswer")
    data class Answer(
        val questionId: Long,
        @field:Schema(description = "학생이 본 문제 번호. 1부터 빈 번호 없이 이어진다")
        val number: Int,
        @field:Schema(description = "정렬 순서. 가운데 문제를 지우면 빈 번호가 생기므로 화면 번호로는 number를 쓴다")
        val displayOrder: Int,
        val type: QuestionType,
        val content: String?,
        @field:Schema(description = "문제 이미지의 서명 URL. 10분간 유효하다")
        val imageUrl: String?,
        val explanation: String?,
        @field:Schema(description = "객관식 보기. 주관식이면 빈 목록")
        val options: List<Option>,
        @field:Schema(description = "주관식 인정 답안. 객관식이면 빈 목록")
        val acceptedAnswers: List<String>,
        val selectedOptionId: Long?,
        val answerText: String?,
        val result: StudentAnswerResult,
        @field:Schema(description = "결과를 고칠 때 쓴다. 시작하지 않은 문제는 null")
        val attemptAnswerId: Long?,
        @get:JsonProperty("isOverridden")
        val isOverridden: Boolean,
        val byTimeout: Boolean,
    ) {
        companion object {
            fun of(dto: StudentLessonAnswerDto) = Answer(
                questionId = dto.questionId,
                number = dto.number,
                displayOrder = dto.displayOrder,
                type = dto.type,
                content = dto.content,
                imageUrl = dto.imageUrl,
                explanation = dto.explanation,
                options = dto.options.map(Option::of),
                acceptedAnswers = dto.acceptedAnswers,
                selectedOptionId = dto.selectedOptionId,
                answerText = dto.answerText,
                result = dto.result,
                attemptAnswerId = dto.attemptAnswerId,
                isOverridden = dto.isOverridden,
                byTimeout = dto.byTimeout,
            )
        }
    }

    @Schema(name = "StudentAnswerResponseOption")
    data class Option(
        val optionId: Long,
        val displayOrder: Int,
        val content: String?,
        val imageUrl: String?,
        @get:JsonProperty("isCorrect")
        val isCorrect: Boolean,
    ) {
        companion object {
            fun of(dto: AnswerOptionDto) = Option(
                optionId = dto.optionId,
                displayOrder = dto.displayOrder,
                content = dto.content,
                imageUrl = dto.imageUrl,
                isCorrect = dto.isCorrect,
            )
        }
    }
}
