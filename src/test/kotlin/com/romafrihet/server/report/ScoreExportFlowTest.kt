package com.romafrihet.server.report

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.report.command.ReportCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ScoreExportFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var scoreExportService: ScoreExportService

    @Test
    fun `학생별 차시 점수는 연 차시마다 열을 두고 응시하지 않은 칸은 비운다`() {
        val classroomId = createClassroom()
        val second = UUID.randomUUID()
        request(classroomId, studentId, "홍길동", "20101")
        request(classroomId, second, "김철수", "20102")
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)

        val file = export(ScoreExportSheet.STUDENT_SCORES, classroomId = classroomId)

        assertThat(file.fileName).isEqualTo("2학년_1반_과학_학생별_차시점수.csv")
        assertThat(rows(file.content)).containsExactly(
            "반,학번,이름,1차시(1문제)",
            "1반,20101,홍길동,1",
            "1반,20102,김철수,",
        )
    }

    @Test
    fun `정오표는 학생마다 문제별 O X와 맞힌 수를 내린다`() {
        val classroomId = joinedClassroom()
        val first = createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, first, correct = false)

        val file = export(ScoreExportSheet.ANSWER_SHEET, lessonNumber = 1)

        assertThat(file.fileName).isEqualTo("2학년_전체반_과학_학생별_정오표_1차시.csv")
        assertThat(rows(file.content)).containsExactly("반,학번,이름,1번,2번,맞힌 수", "1반,20101,홍길동,X,,0")
    }

    @Test
    fun `문제별 정답률은 반마다 문제별 답한 수와 정답률을 내린다`() {
        val classroomId = joinedClassroom()
        val questionId = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, questionId, correct = true)

        val file = export(ScoreExportSheet.QUESTION_RATES, lessonNumber = 1)

        assertThat(rows(file.content)).containsExactly(
            "반,문제,유형,내용,답한 학생,정답,정답률(%)",
            "1반,1,객관식,광합성이 일어나는 곳은?,1,1,100",
        )
    }

    @Test
    fun `문제 단위 표는 차시가 없으면 거절하고 다른 교사나 다른 수업의 반은 404다`() {
        val classroomId = createClassroom()

        assertErrorCode(ErrorCode.INVALID_VALUE) { export(ScoreExportSheet.ANSWER_SHEET) }
        assertErrorCode(ErrorCode.NOT_FOUND) {
            scoreExportService.export(
                ReportCommand.ScoreExport(UUID.randomUUID(), courseId, null, null, ScoreExportSheet.STUDENT_SCORES),
            )
        }
        val otherCourse = createCourse("수학")
        assertErrorCode(ErrorCode.NOT_FOUND) {
            scoreExportService.export(
                ReportCommand.ScoreExport(teacherId, otherCourse, classroomId, null, ScoreExportSheet.STUDENT_SCORES),
            )
        }
    }

    private fun export(sheet: ScoreExportSheet, classroomId: Long? = null, lessonNumber: Int? = null) =
        scoreExportService.export(ReportCommand.ScoreExport(teacherId, courseId, classroomId, lessonNumber, sheet))

    private fun rows(content: ByteArray): List<String> =
        String(content.copyOfRange(3, content.size), Charsets.UTF_8).split("\r\n").filter { it.isNotEmpty() }
}
