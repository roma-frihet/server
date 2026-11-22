package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ClassroomSummaryFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var reportService: ReportService

    @Test
    fun `반마다 학생 수와 열린 차시와 최근 평균을 내린다`() {
        val openClassroom = joinedClassroom(classNo = 1)
        val idleClassroom = createClassroom(classNo = 2)
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(openClassroom, 1)
        submit(openClassroom, studentId, questionId, correct = true)

        val summaries = summaries()

        assertThat(summaries.map { it.classroomId }).containsExactly(openClassroom, idleClassroom)
        val open = summaries.first()
        assertThat(open.studentCount).isEqualTo(1)
        assertThat(open.subjectName).isEqualTo("과학")
        assertThat(open.openLesson?.lessonNumber).isEqualTo(1)
        assertThat(open.openLesson?.title).isEqualTo("1차시 내용")
        assertThat(open.openLesson?.responseCount).isEqualTo(1)
        assertThat(open.latestAverageCorrect).isEqualTo(1.0)
        val idle = summaries.last()
        assertThat(idle.studentCount).isZero()
        assertThat(idle.openLesson).isNull()
        assertThat(idle.latestLessonNumber).isNull()
    }

    @Test
    fun `마감한 차시 중 가장 큰 번호를 내린다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 2)
        openLesson(classroomId, 1)
        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        openLesson(classroomId, 2)

        val summary = summaries().single()

        assertThat(summary.lastFinishedLessonNumber).isEqualTo(1)
        assertThat(summary.openLesson?.lessonNumber).isEqualTo(2)
    }

    @Test
    fun `열려 있는 차시의 미응답은 빼고 닫은 뒤에도 답하지 않았으면 살펴볼 학생으로 센다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)

        assertThat(summaries().single().watchStudentCount).isZero()

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        assertThat(summaries().single().watchStudentCount).isEqualTo(1)
    }

    @Test
    fun `수업이 여러 개면 수업마다 따로 읽어 차시 제목과 문제 수가 섞이지 않는다`() {
        val science = joinedClassroom(classNo = 1)
        val math = createCourse("수학")
        addLesson(math, "수학 1차시")
        val mathClassroom =
            classroomService.create(ClassroomCommand.Create(teacherId, math, 1)).id
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1, courseId = math)
        createQuestion(lessonNumber = 1, courseId = math)
        openLesson(science, 1)
        openLesson(mathClassroom, 1)

        val byId = summaries().associateBy { it.classroomId }

        assertThat(byId.getValue(science).openLesson?.title).isEqualTo("1차시 내용")
        assertThat(byId.getValue(science).openLesson?.questionCount).isEqualTo(1)
        assertThat(byId.getValue(mathClassroom).subjectName).isEqualTo("수학")
        assertThat(byId.getValue(mathClassroom).openLesson?.title).isEqualTo("수학 1차시")
        assertThat(byId.getValue(mathClassroom).openLesson?.questionCount).isEqualTo(2)
    }

    @Test
    fun `다른 교사의 반은 내리지 않고 반이 없으면 빈 목록이다`() {
        joinedClassroom()

        val others = reportService.findClassroomSummaries(ReportCommand.ClassroomSummaries(UUID.randomUUID()))

        assertThat(others).isEmpty()
    }

    private fun summaries() = reportService.findClassroomSummaries(ReportCommand.ClassroomSummaries(teacherId))
}
