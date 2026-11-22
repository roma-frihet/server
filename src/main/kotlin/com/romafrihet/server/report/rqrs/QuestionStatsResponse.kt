package com.romafrihet.server.report.rqrs

import com.fasterxml.jackson.annotation.JsonProperty
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.report.dto.AnswerTextStatDto
import com.romafrihet.server.report.dto.ClassroomQuestionStatsDto
import com.romafrihet.server.report.dto.OptionStatDto
import com.romafrihet.server.report.dto.QuestionStatDto
import io.swagger.v3.oas.annotations.media.Schema

class QuestionStatsResponse private constructor() {
    @Schema(name = "QuestionStatsResponseClassroom")
    data class Classroom(
        val classroomId: Long,
        val classNo: Int,
        val status: LessonStatus,
        val studentCount: Int,
        @field:Schema(description = "이 차시에 하나라도 답한 학생 수")
        val responseCount: Int,
        val questions: List<Question>,
    ) {
        companion object {
            fun of(dto: ClassroomQuestionStatsDto) = Classroom(
                classroomId = dto.classroomId,
                classNo = dto.classNo,
                status = dto.status,
                studentCount = dto.studentCount,
                responseCount = dto.responseCount,
                questions = dto.questions.map(Question::of),
            )
        }
    }

    @Schema(name = "QuestionStatsResponseQuestion")
    data class Question(
        val questionId: Long,
        val displayOrder: Int,
        val type: QuestionType,
        val content: String?,
        val hasImage: Boolean,
        @field:Schema(description = "답을 낸 응답 수. 시간이 지나 미응답으로 확정된 것은 빠진다")
        val answeredCount: Int,
        val rightCount: Int,
        val noAnswerCount: Int,
        @field:Schema(description = "객관식 보기별 응답 수. 주관식이면 빈 목록")
        val options: List<Option>,
        @field:Schema(description = "주관식 인정 답안. 객관식이면 빈 목록")
        val acceptedAnswers: List<String>,
        @field:Schema(description = "주관식 답을 정규화(공백 제거, 소문자)해 묶은 분포. 많은 순")
        val answerTexts: List<AnswerText>,
    ) {
        companion object {
            fun of(dto: QuestionStatDto) = Question(
                questionId = dto.questionId,
                displayOrder = dto.displayOrder,
                type = dto.type,
                content = dto.content,
                hasImage = dto.hasImage,
                answeredCount = dto.answeredCount,
                rightCount = dto.rightCount,
                noAnswerCount = dto.noAnswerCount,
                options = dto.options.map(Option::of),
                acceptedAnswers = dto.acceptedAnswers,
                answerTexts = dto.answerTexts.map(AnswerText::of),
            )
        }
    }

    @Schema(name = "QuestionStatsResponseOption")
    data class Option(
        val optionId: Long,
        val displayOrder: Int,
        val content: String?,
        val hasImage: Boolean,
        @get:JsonProperty("isCorrect")
        val isCorrect: Boolean,
        val count: Int,
    ) {
        companion object {
            fun of(dto: OptionStatDto) = Option(
                optionId = dto.optionId,
                displayOrder = dto.displayOrder,
                content = dto.content,
                hasImage = dto.hasImage,
                isCorrect = dto.isCorrect,
                count = dto.count,
            )
        }
    }

    @Schema(name = "QuestionStatsResponseAnswerText")
    data class AnswerText(
        @field:Schema(description = "묶인 답 중 가장 많이 쓴 원문. 일괄 인정에 그대로 넘긴다")
        val answerText: String,
        val count: Int,
        @field:Schema(description = "그중 정답으로 처리된 수. count와 같으면 모두 인정된 답")
        val rightCount: Int,
    ) {
        companion object {
            fun of(dto: AnswerTextStatDto) = AnswerText(
                answerText = dto.answerText,
                count = dto.count,
                rightCount = dto.rightCount,
            )
        }
    }
}
