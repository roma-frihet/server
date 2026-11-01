package com.romafrihet.server

import com.ninjasquad.springmockk.MockkBean
import com.romafrihet.server.classroom.ClassroomMembership
import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomMembershipService
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.ClassroomService
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.BusinessException
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.course.CourseService
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.lesson.LessonOpeningService
import com.romafrihet.server.lesson.LessonService
import com.romafrihet.server.lesson.QuestionImage
import com.romafrihet.server.lesson.QuestionImageRepository
import com.romafrihet.server.lesson.QuestionService
import com.romafrihet.server.lesson.QuestionType
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.command.QuestionCommand
import com.romafrihet.server.school.SchoolService
import com.romafrihet.server.storage.StorageClient
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/** 실제 스키마로 교사 기능을 검증하는 통합 테스트의 공통 준비다. */
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["app.security.supabase-url=http://localhost:54321"])
@Testcontainers(disabledWithoutDocker = true)
abstract class TeacherFlowFixture {
    @Autowired
    protected lateinit var schoolService: SchoolService

    @Autowired
    protected lateinit var courseService: CourseService

    @Autowired
    protected lateinit var lessonService: LessonService

    @Autowired
    protected lateinit var classroomService: ClassroomService

    @Autowired
    protected lateinit var classroomRepository: ClassroomRepository

    @Autowired
    protected lateinit var membershipService: ClassroomMembershipService

    @Autowired
    protected lateinit var membershipRepository: ClassroomMembershipRepository

    @Autowired
    protected lateinit var questionService: QuestionService

    @Autowired
    protected lateinit var imageRepository: QuestionImageRepository

    @Autowired
    protected lateinit var openingService: LessonOpeningService

    @MockkBean(relaxUnitFun = true)
    protected lateinit var storageClient: StorageClient

    protected val teacherId: UUID = UUID.randomUUID()
    protected val studentId: UUID = UUID.randomUUID()
    protected val schoolId by lazy { schoolService.findAll("다원").single().id }
    protected val courseId by lazy {
        createCourse("과학").also { id -> repeat(LESSON_COUNT) { addLesson(id, "${it + 1}차시 내용") } }
    }

    @BeforeEach
    fun stubStorage() {
        every { storageClient.createSignedUrls(any()) } returns emptyMap()
    }

    protected fun createCourse(subject: String): Long =
        courseService.create(CourseCommand.Create(teacherId, schoolId, 2, subject)).id

    protected fun addLesson(courseId: Long, title: String) =
        lessonService.create(LessonCommand.Create(teacherId, courseId, title))

    protected fun createClassroom(name: String = "1반"): Long =
        classroomService.create(ClassroomCommand.Create(teacherId, courseId, name)).id

    protected fun choice(content: String?, correct: Boolean = false, imageId: Long? = null) =
        QuestionCommand.Option(content, imageId, correct)

    protected fun saveImage(userId: UUID): QuestionImage = imageRepository.save(
        QuestionImage.create(userId, "${UUID.randomUUID()}.png", "image/png", 1),
    )

    protected fun createQuestion(lessonNumber: Int = 3, courseId: Long = this.courseId): Long = questionService.create(
        QuestionCommand.Create(
            userId = teacherId,
            courseId = courseId,
            lessonNumber = lessonNumber,
            type = QuestionType.MULTIPLE_CHOICE,
            content = "광합성이 일어나는 곳은?",
            imageId = null,
            options = listOf(choice("엽록체", correct = true), choice("미토콘드리아")),
        ),
    )

    protected fun request(classroomId: Long, userId: UUID, name: String, number: String): ClassroomMembership =
        membershipRepository.save(
            ClassroomMembership.create(
                classroomRepository.findById(classroomId).orElseThrow(),
                ClassroomMembershipCommand.Create(userId, name, number),
            ),
        )

    protected fun approvedClassroom(name: String = "1반"): Long {
        val classroomId = createClassroom(name)
        val membership = request(classroomId, studentId, "홍길동", "20101")
        membershipService.approve(
            ClassroomMembershipCommand.Decide(teacherId, classroomId, requireNotNull(membership.id)),
        )
        return classroomId
    }

    protected fun assertErrorCode(errorCode: ErrorCode, block: () -> Unit) {
        assertThatThrownBy(block).isInstanceOfSatisfying(BusinessException::class.java) {
            assertThat(it.errorCode).isEqualTo(errorCode)
        }
    }

    protected companion object {
        const val LESSON_COUNT = 5
    }
}
