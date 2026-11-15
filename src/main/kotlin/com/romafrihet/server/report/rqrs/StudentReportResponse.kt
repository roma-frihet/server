package com.romafrihet.server.report.rqrs

import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.report.dto.LessonScoreDto
import com.romafrihet.server.report.dto.OpenLessonDto
import com.romafrihet.server.report.dto.StudentReportDto
import com.romafrihet.server.report.dto.WrongQuestionDto
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 학생 전용 응답이다. 교사용 응답을 재사용하면 다른 학생의 이름과 점수가 새므로 따로 만든다.
 * 반 평균만 담는다.
 */
class StudentReportResponse private constructor() {
    @Schema(name = "StudentReportResponseScore")
    data class Score(
        val lessonNumber: Int,
        @Schema(nullable = true) val correctCount: Int?,
        val questionCount: Int,
        @Schema(nullable = true) val classAverageCorrect: Double?,
    ) {
        companion object {
            fun of(dto: LessonScoreDto) = Score(
                lessonNumber = dto.lessonNumber,
                correctCount = dto.correctCount,
                questionCount = dto.questionCount,
                classAverageCorrect = dto.classAverageCorrect,
            )
        }
    }

    @Schema(name = "StudentReportResponseOpenLesson")
    data class OpenLesson(
        val lessonNumber: Int,
        val title: String,
        val questionCount: Int,
        val totalTimeLimitSec: Int,
        val openedAt: LocalDateTime,
        val phase: AttemptPhase,
        val nextIndex: Int,
    ) {
        companion object {
            fun of(dto: OpenLessonDto) = OpenLesson(
                lessonNumber = dto.lessonNumber,
                title = dto.title,
                questionCount = dto.questionCount,
                totalTimeLimitSec = dto.totalTimeLimitSec,
                openedAt = dto.openedAt,
                phase = dto.phase,
                nextIndex = dto.nextIndex,
            )
        }
    }

    @Schema(name = "StudentReportResponseWrongQuestion")
    data class WrongQuestion(
        val lessonNumber: Int,
        val lessonTitle: String,
        val questionIndex: Int,
        val questionId: Long,
        val content: String?,
        val result: QuestionResult,
        @Schema(nullable = true) val correctAnswer: String?,
        @Schema(nullable = true) val explanation: String?,
    ) {
        companion object {
            fun of(dto: WrongQuestionDto) = WrongQuestion(
                lessonNumber = dto.lessonNumber,
                lessonTitle = dto.lessonTitle,
                questionIndex = dto.questionIndex,
                questionId = dto.questionId,
                content = dto.content,
                result = dto.result,
                correctAnswer = dto.correctAnswer,
                explanation = dto.explanation,
            )
        }
    }

    @Schema(name = "StudentReportResponseResult")
    data class Result(
        val classroomId: Long,
        val classNo: Int,
        val grade: Int,
        val subjectName: String,
        @Schema(nullable = true) val openLesson: OpenLesson?,
        @Schema(nullable = true) val latestScore: Score?,
        val scores: List<Score>,
        val totalCorrectCount: Int,
        val totalQuestionCount: Int,
        val recentWrongQuestions: List<WrongQuestion>,
    ) {
        companion object {
            fun of(dto: StudentReportDto) = Result(
                classroomId = dto.classroomId,
                classNo = dto.classNo,
                grade = dto.grade,
                subjectName = dto.subjectName,
                openLesson = dto.openLesson?.let(OpenLesson::of),
                latestScore = dto.latestScore?.let(Score::of),
                scores = dto.scores.map(Score::of),
                totalCorrectCount = dto.totalCorrectCount,
                totalQuestionCount = dto.totalQuestionCount,
                recentWrongQuestions = dto.recentWrongQuestions.map(WrongQuestion::of),
            )
        }
    }
}
