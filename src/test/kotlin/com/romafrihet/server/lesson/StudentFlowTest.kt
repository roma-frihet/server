package com.romafrihet.server.lesson

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.MembershipStatus
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.lesson.command.LessonOpeningCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.lesson.command.SubmissionCommand
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
    private lateinit var submissionService: LessonSubmissionService

    private fun saveProfile(userId: UUID = studentId, name: String = "홍길동") = studentProfileService.create(
        StudentProfileCommand.Create(userId, schoolId, 2, 1, seatNumbers.incrementAndGet(), name),
    )

    private fun createShortAnswer(lessonNumber: Int): Long = questionService.create(
        QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = QuestionType.SHORT_ANSWER,
            content = "필요한 기체는?",
            imageId = null,
            options = listOf(choice("이산화탄소", correct = true), choice("CO2", correct = true)),
        ),
    )

    private fun joinedClassroom(name: String = "1반"): Long {
        saveProfile()
        val classroomId = classroomService.create(ClassroomCommand.Create(teacherId, courseId, name)).id
        val code = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, classroomId)).joinCode
        val membership = membershipService.join(ClassroomMembershipCommand.Join(studentId, requireNotNull(code)))
        membershipService.approve(ClassroomMembershipCommand.Decide(teacherId, classroomId, membership.id))
        return classroomId
    }

    @Test
    fun `승인 목록에는 가입 때 입력한 이름과 학번이 보인다`() {
        val profile = saveProfile()
        val classroomId = classroomService.create(ClassroomCommand.Create(teacherId, courseId, "1반")).id
        val code = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, classroomId)).joinCode
        membershipService.join(ClassroomMembershipCommand.Join(studentId, requireNotNull(code)))

        val pending = membershipService
            .findAllForTeacher(ClassroomMembershipCommand.Query(teacherId, classroomId, MembershipStatus.PENDING))
            .single()

        assertThat(pending.studentName).isEqualTo(profile.name)
        assertThat(pending.studentNumber).isEqualTo(profile.studentNumber)
        assertThat(pending.subject).isEqualTo("과학")
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
            studentProfileService.create(
                StudentProfileCommand.Create(UUID.randomUUID(), schoolId, 2, 1, profile.seatNumber, "김철수"),
            )
        }
    }

    @Test
    fun `승인된 학생은 열린 차시의 문제를 조회한다`() {
        val classroomId = joinedClassroom()
        createQuestion()
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 3))

        val questions = studentService.findQuestions(QuestionCommand.StudentQuery(studentId, classroomId, 3))

        assertThat(questions).hasSize(1)
        assertThat(questions.single().options.map { it.content }).containsExactly("엽록체", "미토콘드리아")
    }

    @Test
    fun `열지 않은 차시와 승인되지 않은 학생은 조회할 수 없다`() {
        val classroomId = joinedClassroom()
        createQuestion()

        assertErrorCode(ErrorCode.NOT_FOUND) {
            studentService.findQuestions(QuestionCommand.StudentQuery(studentId, classroomId, 3))
        }
        assertErrorCode(ErrorCode.FORBIDDEN) {
            studentService.findQuestions(QuestionCommand.StudentQuery(UUID.randomUUID(), classroomId, 3))
        }
    }

    @Test
    fun `닫은 차시는 학생에게 숨기고 마감한 차시는 목록에 보인다`() {
        val classroomId = joinedClassroom()
        createQuestion(lessonNumber = 1)
        val query = QuestionCommand.StudentQuery(studentId, classroomId, 1)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))

        openingService.close(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertErrorCode(ErrorCode.NOT_FOUND) { studentService.findQuestions(query) }
        assertThat(studentService.findOpenedLessons(studentId, classroomId)).isEmpty()

        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        assertThat(studentService.findOpenedLessons(studentId, classroomId).single().status)
            .isEqualTo(LessonStatus.FINISHED)
    }

    @Test
    fun `제출은 한 번이고 마감 후 내 답과 정답을 볼 수 있다`() {
        val classroomId = joinedClassroom()
        val multipleChoice = createQuestion(lessonNumber = 1)
        val shortAnswer = createShortAnswer(lessonNumber = 1)
        val correctOption = questionService
            .find(QuestionCommand.Get(teacherId, multipleChoice))
            .options
            .first { it.isCorrect }
            .id
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        val answers =
            listOf(
                SubmissionCommand.Answer(multipleChoice, correctOption, null),
                SubmissionCommand.Answer(shortAnswer, null, " co 2 "),
            )
        val submit = SubmissionCommand.Create(studentId, classroomId, 1, answers)
        val result = SubmissionCommand.Query(studentId, classroomId, 1)

        assertErrorCode(ErrorCode.NOT_FOUND) { submissionService.findResult(result) }
        submissionService.submit(submit)

        assertErrorCode(ErrorCode.SUBMISSION_ALREADY_EXISTS) { submissionService.submit(submit) }
        openingService.finish(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        val found = submissionService.findResult(result)
        assertThat(found.correctCount).isEqualTo(2)
        assertThat(found.questions.map { it.isCorrect }).containsExactly(true, true)
        assertErrorCode(ErrorCode.LESSON_NOT_OPEN) { submissionService.submit(submit) }
    }

    @Test
    fun `이 차시 문제가 아닌 답안은 거절하고 대기 학생은 제출할 수 없다`() {
        val classroomId = joinedClassroom()
        val question = createQuestion(lessonNumber = 1)
        val other = createQuestion(lessonNumber = 2)
        openingService.open(LessonOpeningCommand.Change(teacherId, classroomId, 1))
        val answer = SubmissionCommand.Answer(other, null, null)

        assertErrorCode(ErrorCode.SUBMISSION_ANSWER_INVALID) {
            submissionService.submit(SubmissionCommand.Create(studentId, classroomId, 1, listOf(answer)))
        }
        assertErrorCode(ErrorCode.SUBMISSION_ANSWER_INVALID) {
            val duplicate = SubmissionCommand.Answer(question, null, null)
            submissionService.submit(
                SubmissionCommand.Create(studentId, classroomId, 1, listOf(duplicate, duplicate)),
            )
        }
        assertErrorCode(ErrorCode.FORBIDDEN) {
            submissionService.submit(SubmissionCommand.Create(UUID.randomUUID(), classroomId, 1, emptyList()))
        }
    }

    private companion object {
        val seatNumbers = AtomicInteger()
    }
}
