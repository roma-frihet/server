package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.LessonSubmission
import com.romafrihet.server.lesson.LessonSubmissionRepository
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.report.command.ReportCommand
import com.romafrihet.server.teacher.TeacherService
import com.romafrihet.server.teacher.command.TeacherCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ReportTeacherFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var teacherService: TeacherService

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var submissionRepository: LessonSubmissionRepository

    /** 학생 제출 API 없이 채점 결과만 만든다. */
    private fun submit(classroomId: Long, userId: UUID, questionId: Long, correct: Boolean) {
        val question = requireNotNull(questionRepository.findWithOptions(questionId, teacherId))
        val option = question.options.first { it.isCorrect == correct }
        val submission =
            LessonSubmission.create(
                classroomRepository.findById(classroomId).orElseThrow(),
                question.lessonNumber,
                userId,
                1,
            )
        submission.addAnswer(question, option.id, null)
        submissionRepository.save(submission)
    }

    private fun change(classroomId: Long, lessonNumber: Int) =
        LessonOpeningCommand.Change(teacherId, classroomId, lessonNumber)

    @Test
    fun `대시보드와 학생 관리는 점수 하락과 미응답을 알려 준다`() {
        val classroomId = approvedClassroom()
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
        val classroomId = approvedClassroom()
        val second = UUID.randomUUID()
        val pending = requireNotNull(request(classroomId, second, "김영희", "20102").id)
        membershipService.approve(ClassroomMembershipCommand.Decide(teacherId, classroomId, pending))
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
    fun `마이페이지는 수업과 반을 보여 주고 기록이 있는 반은 지우지 않는다`() {
        val classroomId = approvedClassroom()
        teacherService.saveProfile(TeacherCommand.SaveProfile(teacherId, schoolId, "김선생"))
        val emptyCourse = createCourse("미술")

        val myPage = teacherService.findMyPage(teacherId)
        assertThat(myPage.name).isEqualTo("김선생")
        assertThat(myPage.courses.first { it.id == courseId }.lessonCount).isEqualTo(LESSON_COUNT)

        assertErrorCode(ErrorCode.CLASSROOM_HAS_RECORDS) {
            teacherService.deleteClassroom(TeacherCommand.DeleteClassroom(teacherId, classroomId))
        }
        assertErrorCode(ErrorCode.COURSE_HAS_RECORDS) {
            teacherService.deleteCourse(TeacherCommand.DeleteCourse(teacherId, courseId))
        }
        teacherService.deleteCourse(TeacherCommand.DeleteCourse(teacherId, emptyCourse))
        assertThat(teacherService.findMyPage(teacherId).courses.map { it.id }).doesNotContain(emptyCourse)
    }
}
