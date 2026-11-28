package com.romafrihet.server.admin

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.security.SupabaseAuthClient
import com.romafrihet.server.student.StudentLoginEmailPolicy
import com.romafrihet.server.student.StudentProfileRepository
import com.romafrihet.server.student.StudentProfileService
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.teacher.TeacherApplicationStatus
import com.romafrihet.server.teacher.TeacherService
import com.romafrihet.server.teacher.command.TeacherCommand
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.random.Random

class AdminManagementFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var teacherAdminService: TeacherAdminService

    @Autowired
    private lateinit var studentAdminService: StudentAdminService

    @Autowired
    private lateinit var questionAdminService: QuestionAdminService

    @Autowired
    private lateinit var suspensionService: AccountSuspensionService

    @Autowired
    private lateinit var teacherService: TeacherService

    @Autowired
    private lateinit var studentProfileService: StudentProfileService

    @Autowired
    private lateinit var studentProfileRepository: StudentProfileRepository

    @Autowired
    private lateinit var loginEmailPolicy: StudentLoginEmailPolicy

    @MockkBean(relaxUnitFun = true)
    private lateinit var supabaseAuthClient: SupabaseAuthClient

    private val adminId = UUID.randomUUID()

    /** 테스트끼리 DB를 함께 쓰므로 이름으로 이 테스트의 행만 거른다. */
    private val teacherName = "관리${Random.nextInt(1_000_000)}"

    @Test
    fun `교사 목록은 수업 수와 정지 여부를 함께 내린다`() {
        teacherService.saveProfile(TeacherCommand.SaveProfile(teacherId, schoolId, teacherName))
        createCourse("과학")
        createCourse("수학")

        suspensionService.suspend(AdminCommand.Suspend(adminId, teacherId, "확인 필요"))

        val teacher = teachers().single()
        assertThat(teacher.userId).isEqualTo(teacherId)
        assertThat(teacher.courseCount).isEqualTo(2)
        assertThat(teacher.status).isEqualTo(TeacherApplicationStatus.APPROVED)
        assertThat(teacher.email).isNull()
        assertThat(teacher.isSuspended).isTrue()
        verify { supabaseAuthClient.updateBan(teacherId, banned = true) }

        suspensionService.unsuspend(AdminCommand.Unsuspend(adminId, teacherId))

        assertThat(teachers().single().isSuspended).isFalse()
        verify { supabaseAuthClient.updateBan(teacherId, banned = false) }
    }

    @Test
    fun `같은 계정을 다시 정지하면 사유만 바뀌고 자기 계정은 정지하지 못한다`() {
        suspensionService.suspend(AdminCommand.Suspend(adminId, teacherId, "처음"))
        suspensionService.suspend(AdminCommand.Suspend(adminId, teacherId, "다시"))

        assertErrorCode(ErrorCode.ADMIN_SELF_ACTION) {
            suspensionService.suspend(AdminCommand.Suspend(adminId, adminId, null))
        }
        suspensionService.unsuspend(AdminCommand.Unsuspend(adminId, teacherId))
    }

    @Test
    fun `학생 목록은 학년과 반으로 거르고 학번을 붙인다`() {
        val classNumber = Random.nextInt(100, 100_000)
        saveStudent(studentId, classNumber, seat = 2, name = "이학생")
        saveStudent(UUID.randomUUID(), classNumber, seat = 1, name = "김학생")
        saveStudent(UUID.randomUUID(), classNumber + 1, seat = 1, name = "다른반")

        val students =
            studentAdminService
                .findAll(AdminCommand.StudentQuery(schoolId, 3, classNumber, null, 0, 20))
                .content

        assertThat(students.map { it.name }).containsExactly("김학생", "이학생")
        assertThat(students.last().studentNumber).isEqualTo("3%02d02".format(classNumber))
        assertThat(students.last().isSuspended).isFalse()
    }

    @Test
    fun `학생 비밀번호를 초기화하면 임시 비밀번호를 내리고 없는 학생은 404다`() {
        saveStudent(studentId, Random.nextInt(100, 100_000), seat = 1, name = "홍길동")

        val temporary = studentAdminService.resetPassword(studentId)

        verify { supabaseAuthClient.updatePassword(studentId, temporary) }
        assertThat(requireNotNull(studentProfileRepository.findByUserId(studentId)).mustChangePassword).isTrue()
        assertErrorCode(ErrorCode.NOT_FOUND) { studentAdminService.resetPassword(UUID.randomUUID()) }
    }

    @Test
    fun `문제 목록은 교사로 거르고 교사 이름과 차시 제목을 붙인다`() {
        teacherService.saveProfile(TeacherCommand.SaveProfile(teacherId, schoolId, teacherName))
        createQuestion(lessonNumber = 1)
        val later = createQuestion(lessonNumber = 2)

        val questions = questions(teacherUserId = teacherId)

        assertThat(questions.map { it.questionId }.first()).isEqualTo(later)
        val first = questions.first()
        assertThat(first.teacherName).isEqualTo(teacherName)
        assertThat(first.lessonTitle).isEqualTo("2차시 내용")
        assertThat(first.subjectName).isEqualTo("과학")
        assertThat(first.type).isEqualTo(QuestionType.MULTIPLE_CHOICE)
        assertThat(questions(teacherUserId = teacherId, keyword = "광합성")).hasSize(2)
        assertThat(questions(teacherUserId = teacherId, keyword = "없는 글")).isEmpty()
    }

    @Test
    fun `관리자는 다른 교사의 문제를 지우고 지금 열린 차시의 문제는 막힌다`() {
        val classroomId = joinedClassroom()
        val solved = createQuestion(lessonNumber = 1)
        openLesson(classroomId, 1)
        submit(classroomId, studentId, solved, correct = true)

        assertErrorCode(ErrorCode.LESSON_OPEN_NOW) {
            questionService.deleteAsAdmin(QuestionCommand.AdminDelete(solved))
        }
        closeLesson(classroomId, 1)
        questionService.deleteAsAdmin(QuestionCommand.AdminDelete(solved))

        assertThat(questionRepository.findById(solved)).isEmpty()
        assertErrorCode(ErrorCode.NOT_FOUND) { questionService.deleteAsAdmin(QuestionCommand.AdminDelete(-1)) }
    }

    private fun teachers() =
        teacherAdminService.findAll(AdminCommand.TeacherQuery(schoolId, teacherName, 0, 20)).content

    private fun questions(teacherUserId: UUID, keyword: String? = null) = questionAdminService
        .findAll(AdminCommand.QuestionQuery(null, null, teacherUserId, null, keyword, 0, 20))
        .content

    private fun saveStudent(userId: UUID, classNumber: Int, seat: Int, name: String) {
        val command = StudentProfileCommand.Create(userId, schoolId, 3, classNumber, seat, name, null)
        studentProfileService.create(command.copy(loginEmail = loginEmailPolicy.of(command)))
    }
}
