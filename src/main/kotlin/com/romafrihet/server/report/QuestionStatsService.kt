package com.romafrihet.server.report

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.AttemptAnswerRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.Question
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.getByNumber
import com.romafrihet.server.lesson.normalizeAnswer
import com.romafrihet.server.lesson.projection.AnswerCountProjection
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.AnswerTextStatDto
import com.romafrihet.server.report.dto.ClassroomQuestionStatsDto
import com.romafrihet.server.report.dto.OptionStatDto
import com.romafrihet.server.report.dto.QuestionStatDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 문제별 정답률과 응답 분포다. 차시 상세, 반 비교, 수업 중 화면이 같은 값을 쓴다. */
@Service
@Transactional(readOnly = true)
class QuestionStatsService(
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonRepository: LessonRepository,
    private val questionRepository: QuestionRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val classroomRecordsReader: ClassroomRecordsReader,
) {
    /** 읽기 전에 시간이 지난 응답을 확정하므로 readOnly가 아니다. */
    @Transactional
    fun findQuestionStats(command: ReportCommand.QuestionStats): List<ClassroomQuestionStatsDto> {
        courseRepository.getOwned(command.courseId, command.userId)
        lessonRepository.getByNumber(command.courseId, command.lessonNumber)
        val classrooms =
            if (command.classroomIds.isEmpty()) {
                classroomRepository.findAllByCourseIdOrderByClassNoAsc(command.courseId)
            } else {
                classroomRepository.findAllByCourseIdAndIdInOrderByClassNoAsc(command.courseId, command.classroomIds)
            }
        if (classrooms.isEmpty()) return emptyList()
        val classroomIds = classrooms.map { requireNotNull(it.id) }
        val records = classroomRecordsReader.read(command.courseId, classroomIds)
        val questions = questionRepository.findAllWithOptions(command.courseId, command.lessonNumber)
        val counts =
            attemptAnswerRepository
                .findAnswerCounts(classroomIds, command.lessonNumber)
                .groupBy { it.classroomId to it.questionId }
        return classrooms.map { toClassroomStats(it, command.lessonNumber, records, questions, counts) }
    }

    private fun toClassroomStats(
        classroom: Classroom,
        lessonNumber: Int,
        records: ClassroomRecords,
        questions: List<Question>,
        counts: Map<Pair<Long, Long>, List<AnswerCountProjection>>,
    ): ClassroomQuestionStatsDto {
        val classroomId = requireNotNull(classroom.id)
        val opening = records.openings[classroomId].orEmpty().firstOrNull { it.lessonNumber == lessonNumber }
        val scores = records.scores[classroomId].orEmpty()
        return ClassroomQuestionStatsDto(
            classroomId = classroomId,
            classNo = classroom.classNo,
            status = opening?.status ?: LessonStatus.PLANNED,
            studentCount = records.students[classroomId].orEmpty().size,
            responseCount = scores.count { (_, byLesson) -> (byLesson[lessonNumber]?.answeredCount ?: 0) > 0 },
            questions =
            questions.map { question ->
                toQuestionStat(question, counts[classroomId to requireNotNull(question.id)].orEmpty())
            },
        )
    }

    private fun toQuestionStat(question: Question, rows: List<AnswerCountProjection>): QuestionStatDto {
        fun sumOf(predicate: (AnswerCountProjection) -> Boolean) =
            rows.filter(predicate).sumOf { it.answerCount }.toInt()
        val options = question.options.sortedBy { it.displayOrder }
        val isChoice = question.type == QuestionType.MULTIPLE_CHOICE
        return QuestionStatDto(
            questionId = requireNotNull(question.id),
            displayOrder = question.displayOrder,
            type = question.type,
            content = question.content,
            hasImage = question.imageId != null,
            answeredCount = sumOf { it.finalResult != QuestionResult.NONE },
            rightCount = sumOf { it.finalResult == QuestionResult.RIGHT },
            noAnswerCount = sumOf { it.finalResult == QuestionResult.NONE },
            options =
            if (isChoice) {
                options.map { option ->
                    OptionStatDto(
                        optionId = requireNotNull(option.id),
                        displayOrder = option.displayOrder,
                        content = option.content,
                        hasImage = option.imageId != null,
                        isCorrect = option.isCorrect,
                        count = sumOf { it.optionId == option.id },
                    )
                }
            } else {
                emptyList()
            },
            acceptedAnswers = if (isChoice) emptyList() else options.mapNotNull { it.content },
            answerTexts = if (isChoice) emptyList() else toAnswerTexts(rows),
        )
    }

    private fun toAnswerTexts(rows: List<AnswerCountProjection>): List<AnswerTextStatDto> = rows
        .filter { !it.answerText.isNullOrBlank() }
        .groupBy { normalizeAnswer(requireNotNull(it.answerText)) }
        .map { (_, group) ->
            AnswerTextStatDto(
                answerText = representative(group),
                count = group.sumOf { it.answerCount }.toInt(),
                rightCount = group.filter { it.finalResult == QuestionResult.RIGHT }.sumOf { it.answerCount }.toInt(),
            )
        }.sortedByDescending { it.count }

    /** 같은 결과로 묶인 원문 중 가장 많이 쓴 것을, 수가 같으면 더 짧은(공백이 적은) 것을 고른다. */
    private fun representative(group: List<AnswerCountProjection>): String = group
        .groupBy { requireNotNull(it.answerText) }
        .mapValues { (_, rows) -> rows.sumOf { it.answerCount } }
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key.length })
        .first()
        .key
}
