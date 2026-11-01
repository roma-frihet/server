package com.romafrihet.server.report.rqrs

import com.romafrihet.server.common.web.PageInfo
import com.romafrihet.server.lesson.LessonStatus
import com.romafrihet.server.report.StudentProgressStatus
import com.romafrihet.server.report.dto.ClassroomProgressDto
import com.romafrihet.server.report.dto.DashboardClassroomDto
import com.romafrihet.server.report.dto.DashboardDto
import com.romafrihet.server.report.dto.DashboardLessonDto
import com.romafrihet.server.report.dto.DashboardLessonStatusDto
import com.romafrihet.server.report.dto.DashboardSetupDto
import com.romafrihet.server.report.dto.LessonScoreDto
import com.romafrihet.server.report.dto.LiveLessonDto
import com.romafrihet.server.report.dto.StudentLessonScoreDto
import com.romafrihet.server.report.dto.StudentProgressDto
import com.romafrihet.server.report.dto.WatchStudentDto
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

class ReportResponse private constructor() {
    @Schema(name = "ReportResponseScore")
    data class Score(
        val lessonNumber: Int,
        val correctCount: Int?,
        val questionCount: Int,
        val classAverageCorrect: Double?,
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

    @Schema(name = "ReportResponseStudent")
    data class Student(
        val membershipId: Long,
        val studentName: String,
        val studentNumber: String,
        val recentScores: List<Score>,
        val latestCorrectCount: Int?,
        val previousCorrectCount: Int?,
        val totalCorrectCount: Int,
        val totalQuestionCount: Int,
        val status: StudentProgressStatus,
    ) {
        companion object {
            fun of(dto: StudentProgressDto) = Student(
                membershipId = dto.membershipId,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                recentScores = dto.recentScores.map(Score::of),
                latestCorrectCount = dto.latestCorrectCount,
                previousCorrectCount = dto.previousCorrectCount,
                totalCorrectCount = dto.totalCorrectCount,
                totalQuestionCount = dto.totalQuestionCount,
                status = dto.status,
            )
        }
    }

    @Schema(name = "ReportResponseClassroom")
    data class Classroom(
        val classroomId: Long,
        val classroomName: String,
        val studentCount: Int,
        val latestLessonNumber: Int?,
        val latestQuestionCount: Int,
        val latestAverageCorrect: Double?,
        val latestResponseCount: Int,
        val watchStudentCount: Int,
        val students: List<Student>,
    ) {
        companion object {
            fun of(dto: ClassroomProgressDto) = Classroom(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                studentCount = dto.studentCount,
                latestLessonNumber = dto.latestLessonNumber,
                latestQuestionCount = dto.latestQuestionCount,
                latestAverageCorrect = dto.latestAverageCorrect,
                latestResponseCount = dto.latestResponseCount,
                watchStudentCount = dto.watchStudentCount,
                students = dto.students.map(Student::of),
            )
        }
    }

    @Schema(name = "ReportResponseStudentScore")
    data class StudentScore(
        val classroomId: Long,
        val classroomName: String,
        val membershipId: Long,
        val studentName: String,
        val studentNumber: String,
        val lessonNumber: Int,
        val correctCount: Int?,
        val questionCount: Int?,
    ) {
        companion object {
            fun of(dto: StudentLessonScoreDto) = StudentScore(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                membershipId = dto.membershipId,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                lessonNumber = dto.lessonNumber,
                correctCount = dto.correctCount,
                questionCount = dto.questionCount,
            )
        }
    }

    @Schema(name = "ReportResponseScoreResultPage")
    data class ScoreResultPage(val content: List<StudentScore>, val pageInfo: PageInfo) {
        companion object {
            fun of(page: Page<StudentLessonScoreDto>) =
                ScoreResultPage(content = page.content.map(StudentScore::of), pageInfo = PageInfo.of(page))
        }
    }

    @Schema(name = "ReportResponseSetup")
    data class Setup(
        val hasClassroom: Boolean,
        val hasLesson: Boolean,
        val hasQuestion: Boolean,
        val hasOpenedLesson: Boolean,
    ) {
        companion object {
            fun of(dto: DashboardSetupDto) = Setup(
                hasClassroom = dto.hasClassroom,
                hasLesson = dto.hasLesson,
                hasQuestion = dto.hasQuestion,
                hasOpenedLesson = dto.hasOpenedLesson,
            )
        }
    }

    @Schema(name = "ReportResponseLiveLesson")
    data class LiveLesson(
        val classroomId: Long,
        val classroomName: String,
        val lessonNumber: Int,
        val lessonTitle: String,
        val responseCount: Int,
        val studentCount: Int,
        val averageCorrect: Double?,
        val questionCount: Int,
    ) {
        companion object {
            fun of(dto: LiveLessonDto) = LiveLesson(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                lessonNumber = dto.lessonNumber,
                lessonTitle = dto.lessonTitle,
                responseCount = dto.responseCount,
                studentCount = dto.studentCount,
                averageCorrect = dto.averageCorrect,
                questionCount = dto.questionCount,
            )
        }
    }

    @Schema(name = "ReportResponseLesson")
    data class Lesson(val lessonNumber: Int, val title: String) {
        companion object {
            fun of(dto: DashboardLessonDto) = Lesson(lessonNumber = dto.lessonNumber, title = dto.title)
        }
    }

    @Schema(name = "ReportResponseLessonStatus")
    data class ClassroomLessonStatus(val lessonNumber: Int, val status: LessonStatus) {
        companion object {
            fun of(dto: DashboardLessonStatusDto) =
                ClassroomLessonStatus(lessonNumber = dto.lessonNumber, status = dto.status)
        }
    }

    @Schema(name = "ReportResponseDashboardClassroom")
    data class DashboardClassroom(
        val classroomId: Long,
        val classroomName: String,
        val latestAverageCorrect: Double?,
        val latestQuestionCount: Int,
        val lessonStatuses: List<ClassroomLessonStatus>,
    ) {
        companion object {
            fun of(dto: DashboardClassroomDto) = DashboardClassroom(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                latestAverageCorrect = dto.latestAverageCorrect,
                latestQuestionCount = dto.latestQuestionCount,
                lessonStatuses = dto.lessonStatuses.map(ClassroomLessonStatus::of),
            )
        }
    }

    @Schema(name = "ReportResponseWatchStudent")
    data class WatchStudent(
        val classroomId: Long,
        val classroomName: String,
        val membershipId: Long,
        val studentName: String,
        val studentNumber: String,
        val previousCorrectCount: Int?,
        val latestCorrectCount: Int?,
    ) {
        companion object {
            fun of(dto: WatchStudentDto) = WatchStudent(
                classroomId = dto.classroomId,
                classroomName = dto.classroomName,
                membershipId = dto.membershipId,
                studentName = dto.studentName,
                studentNumber = dto.studentNumber,
                previousCorrectCount = dto.previousCorrectCount,
                latestCorrectCount = dto.latestCorrectCount,
            )
        }
    }

    @Schema(name = "ReportResponseDashboard")
    data class Dashboard(
        val classroomCount: Int,
        val openLessonCount: Int,
        val averageCorrect: Double?,
        val averageQuestionCount: Double?,
        val setup: Setup,
        val liveLessons: List<LiveLesson>,
        val lessons: List<Lesson>,
        val classrooms: List<DashboardClassroom>,
        val droppedStudents: List<WatchStudent>,
        val notRespondedStudents: List<WatchStudent>,
    ) {
        companion object {
            fun of(dto: DashboardDto) = Dashboard(
                classroomCount = dto.classroomCount,
                openLessonCount = dto.openLessonCount,
                averageCorrect = dto.averageCorrect,
                averageQuestionCount = dto.averageQuestionCount,
                setup = Setup.of(dto.setup),
                liveLessons = dto.liveLessons.map(LiveLesson::of),
                lessons = dto.lessons.map(Lesson::of),
                classrooms = dto.classrooms.map(DashboardClassroom::of),
                droppedStudents = dto.droppedStudents.map(WatchStudent::of),
                notRespondedStudents = dto.notRespondedStudents.map(WatchStudent::of),
            )
        }
    }
}
