package com.romafrihet.server.report

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.AttemptAnswerRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.Question
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.QuestionResult
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.getByNumber
import com.romafrihet.server.lesson.projection.AnswerResultProjection
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.ScoreExportDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** 성적을 CSV로 만든다. 이름과 학번이 들어가므로 내용을 로그에 남기지 않는다. */
@Service
@Transactional(readOnly = true)
class ScoreExportService(
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonRepository: LessonRepository,
    private val questionRepository: QuestionRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val classroomRecordsReader: ClassroomRecordsReader,
) {
    /** 읽기 전에 시간이 지난 응답을 확정하므로 readOnly가 아니다. */
    @Transactional
    fun export(command: ReportCommand.ScoreExport): ScoreExportDto {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        val classrooms =
            if (command.classroomId == null) {
                classroomRepository.findAllByCourseIdOrderByClassNoAsc(command.courseId)
            } else {
                listOf(
                    classroomRepository
                        .getOwnedInCourse(command.classroomId, command.courseId, command.userId),
                )
            }
        assertCheck(!command.sheet.needsLesson || command.lessonNumber != null) { ErrorCode.INVALID_VALUE }
        val lessonNumber = command.lessonNumber?.also { lessonRepository.getByNumber(command.courseId, it) }
        val records = classroomRecordsReader.read(command.courseId, classrooms.map { requireNotNull(it.id) })
        val table =
            when (command.sheet) {
                ScoreExportSheet.STUDENT_SCORES -> studentScores(classrooms, records)
                ScoreExportSheet.QUESTION_RATES -> questionRates(classrooms, requireNotNull(lessonNumber), course)
                ScoreExportSheet.ANSWER_SHEET -> answerSheet(classrooms, records, requireNotNull(lessonNumber), course)
            }
        return ScoreExportDto(
            fileName = fileName(course, classrooms.singleOrNull()?.takeIf { command.classroomId != null }, command),
            content = table.toBytes(),
        )
    }

    /** 학생 × 이 반들에 연 차시. 응시하지 않은 칸은 비운다. */
    private fun studentScores(classrooms: List<Classroom>, records: ClassroomRecords): CsvTable {
        val lessons = records.openings.values.flatten().map { it.lessonNumber }.distinct().sorted()
        val header = STUDENT_COLUMNS + lessons.map { "${it}차시(${records.questionCounts[it] ?: 0}문제)" }
        val rows =
            classrooms.flatMap { classroom ->
                val scores = records.scores[classroom.id].orEmpty()
                students(classroom, records).map { student ->
                    studentCells(classroom, student.studentNumber, student.studentName) +
                        lessons.map { lesson ->
                            scores[student.userId]?.get(lesson)?.takeIf { it.answeredCount > 0 }
                                ?.correctCount
                                ?.toString()
                                .orEmpty()
                        }
                }
            }
        return CsvTable(header, rows)
    }

    private fun questionRates(classrooms: List<Classroom>, lessonNumber: Int, course: Course): CsvTable {
        val questions = questionRepository.findAllWithOptions(requireNotNull(course.id), lessonNumber)
        val results = results(classrooms, lessonNumber)
        val header = listOf("반", "문제", "유형", "내용", "답한 학생", "정답", "정답률(%)")
        val rows =
            classrooms.flatMap { classroom ->
                questions.mapIndexed { index, question ->
                    val mine = results[classroom.id to question.id].orEmpty()
                    val answered = mine.count { it.finalResult != QuestionResult.NONE }
                    val right = mine.count { it.finalResult == QuestionResult.RIGHT }
                    listOf(
                        "${classroom.classNo}반",
                        "${index + 1}",
                        typeLabel(question),
                        question.content.orEmpty(),
                        "$answered",
                        "$right",
                        if (answered == 0) "" else "${right * PERCENT / answered}",
                    )
                }
            }
        return CsvTable(header, rows)
    }

    /** 학생 × 문제. O는 정답, X는 오답, 빈칸은 미응답이나 시작하지 않은 문제다. */
    private fun answerSheet(
        classrooms: List<Classroom>,
        records: ClassroomRecords,
        lessonNumber: Int,
        course: Course,
    ): CsvTable {
        val questions = questionRepository.findAllWithOptions(requireNotNull(course.id), lessonNumber)
        val byStudent =
            results(classrooms, lessonNumber).values.flatten().associateBy { it.userId to it.questionId }
        val header = STUDENT_COLUMNS + questions.indices.map { "${it + 1}번" } + "맞힌 수"
        val rows =
            classrooms.flatMap { classroom ->
                students(classroom, records).map { student ->
                    val marks = questions.map { byStudent[student.userId to it.id]?.finalResult }
                    studentCells(classroom, student.studentNumber, student.studentName) +
                        marks.map(::mark) +
                        "${marks.count { it == QuestionResult.RIGHT }}"
                }
            }
        return CsvTable(header, rows)
    }

    private fun results(
        classrooms: List<Classroom>,
        lessonNumber: Int,
    ): Map<Pair<Long?, Long?>, List<AnswerResultProjection>> = attemptAnswerRepository
        .findResults(classrooms.map { requireNotNull(it.id) }, lessonNumber)
        .groupBy { it.classroomId to it.questionId }

    private fun students(classroom: Classroom, records: ClassroomRecords) =
        records.students[classroom.id].orEmpty().sortedBy { it.studentNumber }

    private fun studentCells(classroom: Classroom, studentNumber: String, studentName: String) =
        listOf("${classroom.classNo}반", studentNumber, studentName)

    private fun mark(result: QuestionResult?): String = when (result) {
        QuestionResult.RIGHT -> "O"
        QuestionResult.WRONG -> "X"
        QuestionResult.NONE, null -> ""
    }

    private fun typeLabel(question: Question): String = when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> "객관식"
        QuestionType.SHORT_ANSWER -> "주관식"
    }

    private fun fileName(course: Course, classroom: Classroom?, command: ReportCommand.ScoreExport): String {
        val target = classroom?.let { "${it.classNo}반" } ?: "전체반"
        val lesson = command.lessonNumber?.takeIf { command.sheet.needsLesson }?.let { "_${it}차시" }.orEmpty()
        return "${course.grade}학년_${target}_${course.subject.name}_${command.sheet.label}$lesson.csv"
    }

    private fun ClassroomRepository.getOwnedInCourse(classroomId: Long, courseId: Long, userId: UUID): Classroom =
        findByIdAndCourseUserId(classroomId, userId)?.takeIf { it.course.id == courseId }
            ?: throw BusinessException(ErrorCode.NOT_FOUND)

    private companion object {
        val STUDENT_COLUMNS = listOf("반", "학번", "이름")
        const val PERCENT = 100
    }
}
