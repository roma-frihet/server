package com.romafrihet.server.report

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.lesson.AttemptAnswerRepository
import com.romafrihet.server.lesson.AttemptPhase
import com.romafrihet.server.lesson.AttemptRepository
import com.romafrihet.server.lesson.LessonOpening
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.LessonScoreDto
import com.romafrihet.server.report.dto.OpenLessonDto
import com.romafrihet.server.report.dto.StudentReportDto
import com.romafrihet.server.report.dto.WrongQuestionDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** 학생이 보는 자기 리포트다. 다른 학생의 이름이나 점수는 절대 담지 않고 반 평균만 준다. */
@Service
@Transactional
class StudentReportService(
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val classroomRecordsReader: ClassroomRecordsReader,
    private val classroomProgressCalculator: ClassroomProgressCalculator,
    private val lessonRepository: LessonRepository,
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
) {
    fun findReport(command: ReportCommand.StudentReport): StudentReportDto {
        val classroom = enrolledClassroom(command.userId, command.classroomId)
        val courseId = requireNotNull(classroom.course.id)
        // 읽기 전에 시간이 지난 응답을 확정한다. 반 평균도 같은 기준으로 맞춘다.
        val records = classroomRecordsReader.read(courseId, listOf(command.classroomId))
        val openings = records.openings[command.classroomId].orEmpty()
        val openedLessons = openings.map(LessonOpening::lessonNumber).sorted()
        val allScores = records.scores[command.classroomId].orEmpty()
        val mine = allScores[command.userId].orEmpty()
        val answered = openedLessons.filter { (mine[it]?.answeredCount ?: 0) > 0 }
        val titles =
            lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId).associate { it.lessonNumber to it.title }

        val scores =
            openedLessons.map { lessonNumber ->
                LessonScoreDto(
                    lessonNumber = lessonNumber,
                    correctCount = mine[lessonNumber]?.takeIf { it.answeredCount > 0 }?.correctCount?.toInt(),
                    questionCount = records.questionCounts[lessonNumber] ?: 0,
                    classAverageCorrect = classroomProgressCalculator.averageCorrect(allScores, lessonNumber),
                )
            }
        return StudentReportDto(
            classroomId = command.classroomId,
            classNo = classroom.classNo,
            grade = classroom.course.grade,
            subjectName = classroom.course.subject.name,
            openLesson = toOpenLesson(openings, titles, records.questionCounts, command, courseId),
            latestScore = scores.lastOrNull(),
            scores = scores,
            totalCorrectCount = answered.sumOf { mine.getValue(it).correctCount.toInt() },
            totalQuestionCount = answered.sumOf { records.questionCounts[it] ?: 0 },
            recentWrongQuestions =
            findWrongQuestions(command, openings, titles).take(RECENT_WRONG_COUNT),
        )
    }

    /** 마감한 차시에서 맞히지 못한 문제를 최근 차시부터 돌려준다. */
    fun findWrongQuestions(command: ReportCommand.StudentReport): List<WrongQuestionDto> {
        val classroom = enrolledClassroom(command.userId, command.classroomId)
        val courseId = requireNotNull(classroom.course.id)
        val openings = classroomRecordsReader.read(courseId, listOf(command.classroomId))
            .openings[command.classroomId]
            .orEmpty()
        val titles =
            lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId).associate { it.lessonNumber to it.title }
        return findWrongQuestions(command, openings, titles)
    }

    private fun findWrongQuestions(
        command: ReportCommand.StudentReport,
        openings: List<LessonOpening>,
        titles: Map<Int, String>,
    ): List<WrongQuestionDto> {
        // 마감 전에는 정답을 공개하지 않으므로 마감한 차시만 본다.
        val finished = openings.filter { it.status == LessonStatus.FINISHED }.map(LessonOpening::lessonNumber)
        if (finished.isEmpty()) return emptyList()
        return attemptAnswerRepository
            .findWrongAnswers(command.classroomId, command.userId, finished)
            .map { answer ->
                val correct = answer.question.options.firstOrNull { it.isCorrect }
                WrongQuestionDto(
                    lessonNumber = answer.attempt.lessonNumber,
                    lessonTitle = titles[answer.attempt.lessonNumber].orEmpty(),
                    questionIndex = answer.questionIndex,
                    questionId = requireNotNull(answer.question.id),
                    content = answer.question.content,
                    result = answer.finalResult,
                    correctAnswer = correct?.content,
                    explanation = answer.question.explanation,
                )
            }
    }

    private fun toOpenLesson(
        openings: List<LessonOpening>,
        titles: Map<Int, String>,
        questionCounts: Map<Int, Int>,
        command: ReportCommand.StudentReport,
        courseId: Long,
    ): OpenLessonDto? {
        val opening = openings.firstOrNull { it.status == LessonStatus.OPEN } ?: return null
        val attempt =
            attemptRepository.findByClassroomIdAndLessonNumberAndUserId(
                command.classroomId,
                opening.lessonNumber,
                command.userId,
            )
        return OpenLessonDto(
            lessonNumber = opening.lessonNumber,
            title = titles[opening.lessonNumber].orEmpty(),
            questionCount = questionCounts[opening.lessonNumber] ?: 0,
            totalTimeLimitSec =
            questionRepository
                .findAllWithOptions(courseId, opening.lessonNumber)
                .sumOf { it.timeLimitSec },
            openedAt = opening.openedAt,
            phase = attempt?.phase ?: AttemptPhase.READY,
            nextIndex = attempt?.nextIndex ?: 0,
        )
    }

    private fun enrolledClassroom(userId: UUID, classroomId: Long): Classroom {
        assertCheck(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, userId)) {
            ErrorCode.FORBIDDEN
        }
        return classroomRepository.findById(classroomId).orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }
    }

    private companion object {
        const val RECENT_WRONG_COUNT = 3
    }
}
