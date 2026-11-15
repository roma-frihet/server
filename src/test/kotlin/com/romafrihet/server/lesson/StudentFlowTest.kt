package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.AttemptCommand
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.student.StudentLoginEmailPolicy
import com.romafrihet.server.student.StudentProfileService
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.student.rqrs.StudentProfileResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class StudentFlowTest : TeacherFlowFixture() {
    @Autowired
    private lateinit var studentProfileService: StudentProfileService

    @Autowired
    private lateinit var studentService: LessonStudentService

    @Autowired
    private lateinit var attemptService: AttemptService

    @Autowired
    private lateinit var studentLoginEmailPolicy: StudentLoginEmailPolicy

    private fun saveProfile(userId: UUID = studentId, name: String = "홍길동") =
        studentProfileService.create(createCommand(userId, seatNumbers.incrementAndGet(), name))

    /** 학번 규칙에 맞는 로그인 계정을 붙여 준다. 실제로는 프론트가 이 규칙대로 가입시킨다. */
    private fun createCommand(userId: UUID, seatNumber: Int, name: String): StudentProfileCommand.Create {
        val command = StudentProfileCommand.Create(userId, schoolId, 2, 1, seatNumber, name, null)
        return command.copy(loginEmail = studentLoginEmailPolicy.of(command))
    }

    private fun createShortAnswer(lessonNumber: Int): Long = questionService.create(
        QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = QuestionType.SHORT_ANSWER,
            content = "필요한 기체는?",
            imageId = null,
            timeLimitSec = 30,
            explanation = null,
            options = listOf(choice("이산화탄소", correct = true), choice("CO2", correct = true)),
        ),
    )

    private fun enrolledClassroom(classNo: Int = 1): Long {
        saveProfile()
        val classroomId = classroomService.create(ClassroomCommand.Create(teacherId, courseId, classNo)).id
        val code = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, classroomId)).joinCode
        membershipService.join(ClassroomMembershipCommand.Join(studentId, requireNotNull(code)))
        return classroomId
    }

    @Test
    fun `반 학생 목록에는 가입 때 입력한 이름과 학번이 보인다`() {
        val profile = saveProfile()
        val classroomId = classroomService.create(ClassroomCommand.Create(teacherId, courseId, 1)).id
        val code = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, classroomId)).joinCode
        membershipService.join(ClassroomMembershipCommand.Join(studentId, requireNotNull(code)))

        val joined = membershipService
            .findAllForTeacher(ClassroomMembershipCommand.Query(teacherId, classroomId))
            .single()

        assertThat(joined.studentName).isEqualTo(profile.name)
        assertThat(joined.studentNumber).isEqualTo(profile.studentNumber)
        assertThat(joined.subjectName).isEqualTo("과학")
    }

    @Test
    fun `내 정보는 저장 전에는 profile이 null이고 저장 후에는 저장한 값이다`() {
        assertThat(StudentProfileResponse.Mine.of(studentProfileService.find(studentId)).profile).isNull()

        val saved = saveProfile()

        assertThat(StudentProfileResponse.Mine.of(studentProfileService.find(studentId)).profile?.studentNumber)
            .isEqualTo(saved.studentNumber)
    }

    @Test
    fun `가입 정보의 학번이 겹치면 거절한다`() {
        val profile = saveProfile()

        assertErrorCode(ErrorCode.STUDENT_NUMBER_DUPLICATED) {
            studentProfileService.create(createCommand(UUID.randomUUID(), profile.seatNumber, "김철수"))
        }
    }

    /** 남의 학번으로 프로필을 만들지 못하게 막는다. */
    @Test
    fun `로그인 계정이 학번 규칙과 다르면 거절한다`() {
        val command = StudentProfileCommand.Create(studentId, schoolId, 2, 1, 1, "홍길동", "someone@else.com")

        assertErrorCode(ErrorCode.STUDENT_LOGIN_EMAIL_MISMATCH) { studentProfileService.create(command) }
    }

    /** 문제별 제한 시간이 뜻을 가지려면 시작 전에는 본문이 보이면 안 된다. */
    @Test
    fun `시작하지 않은 문제는 개수와 제한 시간만 알려 준다`() {
        val classroomId = enrolledClassroom()
        createQuestion(lessonNumber = 1)
        createQuestion(lessonNumber = 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        val attempt = attemptService.findAttempt(AttemptCommand.Query(studentId, classroomId, 1))

        assertThat(attempt.total).isEqualTo(2)
        assertThat(attempt.nextTimeLimitSec).isEqualTo(30)
        assertThat(attempt.current).isNull()
        assertThat(attempt.last).isNull()
    }

    @Test
    fun `열지 않은 차시와 연결되지 않은 학생은 응시할 수 없다`() {
        val classroomId = enrolledClassroom()
        createQuestion(lessonNumber = 1)

        assertErrorCode(ErrorCode.NOT_FOUND) {
            attemptService.findAttempt(AttemptCommand.Query(studentId, classroomId, 1))
        }
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertErrorCode(ErrorCode.FORBIDDEN) {
            attemptService.findAttempt(AttemptCommand.Query(UUID.randomUUID(), classroomId, 1))
        }
    }

    @Test
    fun `닫은 차시는 학생에게 숨기고 마감한 차시는 목록에 보인다`() {
        val classroomId = enrolledClassroom()
        createQuestion(lessonNumber = 1)
        val query = AttemptCommand.Query(studentId, classroomId, 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        openingService.close(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertErrorCode(ErrorCode.LESSON_NOT_OPEN) {
            attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        }
        assertThat(studentService.findOpenedLessons(studentId, classroomId)).isEmpty()

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertThat(studentService.findOpenedLessons(studentId, classroomId).single().status)
            .isEqualTo(LessonStatus.FINISHED)
    }

    @Test
    fun `마감한 차시에서 내 답과 정답과 해설을 본다`() {
        val classroomId = enrolledClassroom()
        val multipleChoice = createQuestion(lessonNumber = 1)
        val shortAnswer = createShortAnswer(lessonNumber = 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        val query = AttemptCommand.Query(studentId, classroomId, 1)

        assertErrorCode(ErrorCode.NOT_FOUND) { attemptService.findResult(query) }

        solveCorrectly(classroomId, index = 0)
        solveCorrectly(classroomId, index = 1)
        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        val found = attemptService.findResult(query)
        assertThat(found.correctCount).isEqualTo(2)
        assertThat(found.questionCount).isEqualTo(2)
        assertThat(found.questions.map { it.result })
            .containsExactly(QuestionResult.RIGHT, QuestionResult.RIGHT)
        assertThat(found.questions.map { it.question.id }).containsExactly(multipleChoice, shortAnswer)
        assertThat(found.questions.first().correctOptionId).isNotNull()
    }

    @Test
    fun `응시하지 않은 학생은 마감 차시에서 점수가 비어 있다`() {
        val classroomId = enrolledClassroom()
        createQuestion(lessonNumber = 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        val found = attemptService.findResult(AttemptCommand.Query(studentId, classroomId, 1))

        assertThat(found.correctCount).isNull()
        assertThat(found.questions.single().result).isEqualTo(QuestionResult.NONE)
    }

    @Test
    fun `이 차시 문제의 보기가 아니면 거절하고 연결되지 않은 학생은 풀 수 없다`() {
        val classroomId = enrolledClassroom()
        createQuestion(lessonNumber = 1)
        val other = createQuestion(lessonNumber = 2)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, 0))
        val otherOptionId =
            questionService.find(QuestionCommand.Get(teacherId, other)).options.first().id

        assertErrorCode(ErrorCode.SUBMISSION_ANSWER_INVALID) {
            attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, 0, otherOptionId, null))
        }
        assertErrorCode(ErrorCode.FORBIDDEN) {
            attemptService.findAttempt(AttemptCommand.Query(UUID.randomUUID(), classroomId, 1))
        }
    }

    private fun solveCorrectly(classroomId: Long, index: Int) {
        val started = attemptService.start(AttemptCommand.Start(studentId, classroomId, 1, index))
        val question = requireNotNull(started.current).question
        val optionId =
            if (question.type == QuestionType.MULTIPLE_CHOICE) question.options.first().id else null
        val text = if (question.type == QuestionType.SHORT_ANSWER) " co 2 " else null
        attemptService.submit(AttemptCommand.Answer(studentId, classroomId, 1, index, optionId, text))
    }

    private companion object {
        val seatNumbers = AtomicInteger()
    }
}
