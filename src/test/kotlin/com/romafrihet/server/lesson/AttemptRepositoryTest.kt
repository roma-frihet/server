package com.romafrihet.server.lesson

import com.romafrihet.server.TestcontainersConfiguration
import com.romafrihet.server.classroom.Classroom
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.config.JpaConfig
import com.romafrihet.server.course.Course
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.subject.SchoolLevel
import com.romafrihet.server.subject.SubjectRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.UUID

/** 응시의 유일 제약과 확정 대상 조회는 DB에 걸려 있으므로 실제 스키마에서 확인한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class AttemptRepositoryTest(
    @Autowired private val attemptRepository: AttemptRepository,
    @Autowired private val attemptAnswerRepository: AttemptAnswerRepository,
    @Autowired private val questionRepository: QuestionRepository,
    @Autowired private val classroomRepository: ClassroomRepository,
    @Autowired private val courseRepository: CourseRepository,
    @Autowired private val schoolRepository: SchoolRepository,
    @Autowired private val subjectRepository: SubjectRepository,
) {
    private val teacherId = UUID.randomUUID()
    private val studentId = UUID.randomUUID()
    private val now: LocalDateTime = LocalDateTime.of(2026, 11, 15, 10, 0)

    @Test
    fun `응답은 문제 순서대로 내려가고 시작 시점의 제한 시간을 보존한다`() {
        val attempt = saveAttempt()
        saveAnswer(attempt, questionIndex = 1, timeLimitSec = 60)
        saveAnswer(attempt, questionIndex = 0, timeLimitSec = 30)

        val answers = attemptAnswerRepository.findAllByAttemptIdOrderByQuestionIndexAsc(requireNotNull(attempt.id))

        assertThat(answers.map { it.questionIndex }).containsExactly(0, 1)
        assertThat(answers.map { it.timeLimitSec }).containsExactly(30, 60)
    }

    @Test
    fun `마감이 지난 미확정 응답만 확정 대상으로 걸린다`() {
        val attempt = saveAttempt()
        saveAnswer(attempt, questionIndex = 0, timeLimitSec = 30)
        val fresh = saveAnswer(attempt, questionIndex = 1, timeLimitSec = MAX_TIME_LIMIT_SEC)
        val done = saveAnswer(attempt, questionIndex = 2, timeLimitSec = 30)
        done.finalizeWith(optionId = null, answerText = null, byTimeout = true, now = now)
        attemptAnswerRepository.saveAndFlush(done)

        val targets = attemptAnswerRepository.findAllByAttemptIdInAndIsFinalizedFalseAndDeadlineLessThan(
            listOf(requireNotNull(attempt.id)),
            now.plusMinutes(1),
        )

        assertThat(targets.map { it.questionIndex }).containsExactly(0)
        assertThat(fresh.isFinalized).isFalse()
    }

    @Test
    fun `한 학생은 한 반의 한 차시에 응시를 하나만 가진다`() {
        saveAttempt()

        assertThatThrownBy { saveAttempt() }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `같은 응시에 같은 문제 순서를 두 번 넣을 수 없다`() {
        val attempt = saveAttempt()
        saveAnswer(attempt, questionIndex = 0, timeLimitSec = 30)

        assertThatThrownBy { saveAnswer(attempt, questionIndex = 0, timeLimitSec = 30) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun saveAttempt(): Attempt =
        attemptRepository.saveAndFlush(Attempt.create(classroom(), LESSON_NUMBER, studentId))

    private fun saveAnswer(attempt: Attempt, questionIndex: Int, timeLimitSec: Int): AttemptAnswer {
        val question = saveQuestion(timeLimitSec)
        return attemptAnswerRepository.saveAndFlush(
            AttemptAnswer.create(attempt, question, questionIndex, now),
        )
    }

    private fun saveQuestion(timeLimitSec: Int): Question {
        val course = course()
        val command = QuestionCommand.Create(
            userId = teacherId,
            courseId = requireNotNull(course.id),
            lessonNumber = LESSON_NUMBER,
            type = QuestionType.MULTIPLE_CHOICE,
            content = "문제",
            imageId = null,
            timeLimitSec = timeLimitSec,
            explanation = null,
            options = listOf(
                QuestionCommand.Option("정답", null, true),
                QuestionCommand.Option("오답", null, false),
            ),
        )
        return questionRepository.saveAndFlush(Question.create(course, questionRepository.count().toInt(), command))
    }

    private val classroomCache by lazy {
        classroomRepository.saveAndFlush(
            Classroom.create(course(), ClassroomCommand.Create(teacherId, requireNotNull(course().id), 1)),
        )
    }

    private fun classroom(): Classroom = classroomCache

    private val courseCache by lazy {
        val school = schoolRepository.findAll().first()
        val subject = subjectRepository
            .findAllBySchoolLevelOrderByDisplayOrderAsc(SchoolLevel.MIDDLE)
            .single { it.name == "과학" }
        val command =
            CourseCommand.Create(teacherId, requireNotNull(school.id), 2, requireNotNull(subject.id))
        courseRepository.saveAndFlush(Course.create(school, subject, command))
    }

    private fun course(): Course = courseCache

    private companion object {
        const val LESSON_NUMBER = 1
        const val MAX_TIME_LIMIT_SEC = 300
    }
}
