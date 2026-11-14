package com.romafrihet.server.report

import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.Lesson
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.lesson.LessonSubmissionRepository
import com.romafrihet.server.lesson.dto.LessonScoreQueryDto
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.report.dto.ClassroomProgressDto
import com.romafrihet.server.report.dto.DashboardClassroomDto
import com.romafrihet.server.report.dto.DashboardDto
import com.romafrihet.server.report.dto.DashboardLessonDto
import com.romafrihet.server.report.dto.DashboardLessonStatusDto
import com.romafrihet.server.report.dto.DashboardSetupDto
import com.romafrihet.server.report.dto.LiveLessonDto
import com.romafrihet.server.report.dto.StudentLessonScoreDto
import com.romafrihet.server.report.dto.WatchStudentDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReportService(
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonRepository: LessonRepository,
    private val lessonSubmissionRepository: LessonSubmissionRepository,
    private val classroomRecordsReader: ClassroomRecordsReader,
    private val classroomProgressCalculator: ClassroomProgressCalculator,
) {
    /** 통계는 반 전체 기준이고, 검색어는 학생 목록에만 적용한다. */
    fun findStudents(command: ReportCommand.Students): ClassroomProgressDto {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        val records =
            classroomRecordsReader.read(requireNotNull(classroom.course.id), listOf(command.classroomId))
        val progress = classroomProgressCalculator.calculate(command.classroomId, classroom.name, records)
        return progress.copy(students = progress.students.filter { command.keyword in it.studentName })
    }

    /** 수업 범위에서만 조회하므로 다른 교사의 반을 지정하면 빈 페이지가 된다. */
    fun findScores(command: ReportCommand.Scores): Page<StudentLessonScoreDto> {
        courseRepository.getOwned(command.courseId, command.userId)
        return lessonSubmissionRepository
            .queryScores(
                LessonScoreQueryDto(
                    courseId = command.courseId,
                    classroomId = command.classroomId,
                    lessonNumber = command.lessonNumber,
                ),
                PageRequest.of(command.page, command.size),
            ).map {
                StudentLessonScoreDto(
                    classroomId = it.classroomId,
                    classroomName = it.classroomName,
                    membershipId = it.membershipId,
                    studentName = it.studentName,
                    studentNumber = it.studentNumber,
                    lessonNumber = it.lessonNumber,
                    correctCount = it.correctCount,
                    questionCount = it.questionCount,
                )
            }
    }

    fun findDashboard(command: ReportCommand.Dashboard): DashboardDto {
        courseRepository.getOwned(command.courseId, command.userId)
        val classrooms = classroomRepository.findAllByCourseIdOrderByNameAsc(command.courseId)
        val lessons = lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(command.courseId)
        val records = classroomRecordsReader.read(command.courseId, classrooms.map { requireNotNull(it.id) })
        val progress =
            classrooms.map { classroomProgressCalculator.calculate(requireNotNull(it.id), it.name, records) }
        val live = toLiveLessons(classrooms, lessons, records)
        return DashboardDto(
            classroomCount = classrooms.size,
            openLessonCount = live.size,
            averageCorrect = live.mapNotNull(LiveLessonDto::averageCorrect).takeIf { it.isNotEmpty() }?.average(),
            averageQuestionCount = live.map { it.questionCount }.takeIf { it.isNotEmpty() }?.average(),
            setup =
            DashboardSetupDto(
                hasClassroom = classrooms.isNotEmpty(),
                hasLesson = lessons.isNotEmpty(),
                hasQuestion = records.questionCounts.isNotEmpty(),
                hasOpenedLesson = records.openings.isNotEmpty(),
            ),
            liveLessons = live,
            lessons = lessons.map { DashboardLessonDto(lessonNumber = it.lessonNumber, title = it.title) },
            classrooms = progress.map { toDashboardClassroom(it, lessons, records) },
            droppedStudents = watchStudents(progress, StudentProgressStatus.SCORE_DROPPED),
            notRespondedStudents = watchStudents(progress, StudentProgressStatus.NOT_RESPONDED),
        )
    }

    private fun toLiveLessons(
        classrooms: List<Classroom>,
        lessons: List<Lesson>,
        records: ClassroomRecords,
    ): List<LiveLessonDto> {
        val titles = lessons.associate { it.lessonNumber to it.title }
        return classrooms.flatMap { classroom ->
            val classroomId = requireNotNull(classroom.id)
            records.openings[classroomId].orEmpty().filter { it.isOpen }.map { opening ->
                val responses =
                    records.submissions[classroomId].orEmpty().filter { it.lessonNumber == opening.lessonNumber }
                LiveLessonDto(
                    classroomId = classroomId,
                    classroomName = classroom.name,
                    lessonNumber = opening.lessonNumber,
                    lessonTitle = titles[opening.lessonNumber].orEmpty(),
                    responseCount = responses.size,
                    studentCount = records.students[classroomId].orEmpty().size,
                    averageCorrect = classroomProgressCalculator.averageCorrect(responses),
                    questionCount = records.questionCounts[opening.lessonNumber] ?: 0,
                )
            }
        }
    }

    private fun toDashboardClassroom(
        progress: ClassroomProgressDto,
        lessons: List<Lesson>,
        records: ClassroomRecords,
    ): DashboardClassroomDto {
        val statuses = records.openings[progress.classroomId].orEmpty().associateBy { it.lessonNumber }
        return DashboardClassroomDto(
            classroomId = progress.classroomId,
            classroomName = progress.classroomName,
            latestAverageCorrect = progress.latestAverageCorrect,
            latestQuestionCount = progress.latestQuestionCount,
            lessonStatuses =
            lessons.map {
                DashboardLessonStatusDto(
                    lessonNumber = it.lessonNumber,
                    status = statuses[it.lessonNumber]?.status ?: LessonStatus.PLANNED,
                )
            },
        )
    }

    private fun watchStudents(
        progress: List<ClassroomProgressDto>,
        status: StudentProgressStatus,
    ): List<WatchStudentDto> = progress.flatMap { classroom ->
        classroom.students.filter { it.status == status }.map {
            WatchStudentDto(
                classroomId = classroom.classroomId,
                classroomName = classroom.classroomName,
                membershipId = it.membershipId,
                studentName = it.studentName,
                studentNumber = it.studentNumber,
                previousCorrectCount = it.previousCorrectCount,
                latestCorrectCount = it.latestCorrectCount,
            )
        }
    }
}
