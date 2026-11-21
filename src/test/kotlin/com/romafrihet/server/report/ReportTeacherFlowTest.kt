package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.teacher.TeacherService
import com.romafrihet.server.teacher.command.TeacherCommand
import com.romafrihet.server.teacher.rqrs.TeacherResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ReportTeacherFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var teacherService: TeacherService

    private fun change(classroomId: Long, lessonNumber: Int) =
        LessonOpeningCommand.Change(teacherId, classroomId, lessonNumber)

    @Test
    fun `대시보드와 학생 관리는 점수 하락과 미응답을 알려 준다`() {
        val classroomId = joinedClassroom()
        val first = createQuestion(lessonNumber = 1)
        val second = createQuestion(lessonNumber = 2)
        createQuestion(lessonNumber = 3)
        openingService.open(change(classroomId, 1))
        submit(classroomId, studentId, first, correct = true)
        openingService.finish(change(classroomId, 1))
        openingService.open(change(classroomId, 2))
        submit(classroomId, studentId, second, correct = false)

        val dropped = reportService.findDashboard(ReportCommand.Dashboard(teacherId, courseId))
        assertThat(dropped.droppedStudents.single().latestCorrectCount).isEqualTo(0)
        assertThat(dropped.liveLessons.single().responseCount).isEqualTo(1)
        assertThat(dropped.setup.hasOpenedLesson).isTrue()

        openingService.finish(change(classroomId, 2))
        openingService.open(change(classroomId, 3))
        val students = reportService.findStudents(ReportCommand.Students(teacherId, classroomId, ""))
        assertThat(students.students.single().status).isEqualTo(StudentProgressStatus.NOT_RESPONDED)
        assertThat(students.students.single().totalCorrectCount).isEqualTo(1)
        assertThat(students.students.single().recentScores.map { it.correctCount }).containsExactly(1, 0, null)
        assertThat(reportService.findStudents(ReportCommand.Students(teacherId, classroomId, "없는이름")).students)
            .isEmpty()
    }

    @Test
    fun `점수는 반과 차시로 거르고 페이지로 나누며 미제출 학생도 보여 준다`() {
        val classroomId = joinedClassroom()
        val second = UUID.randomUUID()
        request(classroomId, second, "김영희", "20102")
        val first = createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openingService.open(change(classroomId, 1))
        submit(classroomId, studentId, first, correct = true)
        openingService.finish(change(classroomId, 1))
        openingService.open(change(classroomId, 2))
        val query = ReportCommand.Scores(teacherId, courseId, classroomId, 1, 0, 1)

        val firstPage = reportService.findScores(query)
        val secondPage = reportService.findScores(query.copy(page = 1))
        val lessonRows = firstPage.content + secondPage.content

        assertThat(firstPage.totalElements).isEqualTo(2)
        assertThat(lessonRows.map { it.studentName }).containsExactlyInAnyOrder("홍길동", "김영희")
        assertThat(lessonRows.first { it.studentName == "홍길동" }.correctCount).isEqualTo(1)
        assertThat(lessonRows.first { it.studentName == "김영희" }.correctCount).isNull()
        assertThat(reportService.findScores(query.copy(lessonNumber = null, size = 20)).totalElements).isEqualTo(4)
        assertThat(reportService.findScores(query.copy(classroomId = -1L)).totalElements).isZero()
        assertErrorCode(ErrorCode.NOT_FOUND) { reportService.findScores(query.copy(userId = UUID.randomUUID())) }
    }

    @Test
    fun `마이페이지는 수업과 반을 보여 주고 빈 수업을 지운다`() {
        joinedClassroom()
        teacherService.saveProfile(TeacherCommand.SaveProfile(teacherId, schoolId, "김선생"))
        val emptyCourse = createCourse("미술")

        val myPage = teacherService.findMyPage(teacherId)
        assertThat(myPage.profile?.name).isEqualTo("김선생")
        assertThat(myPage.courses.first { it.id == courseId }.lessonCount).isEqualTo(LESSON_COUNT)

        teacherService.deleteCourse(TeacherCommand.DeleteCourse(teacherId, emptyCourse))
        assertThat(teacherService.findMyPage(teacherId).courses.map { it.id }).doesNotContain(emptyCourse)
    }

    @Test
    fun `프로필을 저장하기 전 마이페이지는 프로필 없이 수업을 보여 준다`() {
        val course = createCourse("미술")

        val myPage = TeacherResponse.MyPage.of(teacherService.findMyPage(teacherId))

        assertThat(myPage.profile).isNull()
        assertThat(myPage.courses.map { it.id }).containsExactly(course)
    }
}
