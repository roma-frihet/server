package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.command.LessonCommand
import com.romafrihet.server.lesson.dto.LessonClassroomStatusDto
import com.romafrihet.server.lesson.dto.LessonDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LessonService(
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val lessonRepository: LessonRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
) {
    @Transactional
    fun create(command: LessonCommand.Create): LessonDto {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        val lessonNumber = (lessonRepository.findMaxLessonNumber(command.courseId) ?: 0) + 1
        lessonRepository.save(Lesson.create(course, lessonNumber, command))
        return find(command.courseId).single { it.lessonNumber == lessonNumber }
    }

    @Transactional
    fun rename(command: LessonCommand.Rename) {
        courseRepository.getOwned(command.courseId, command.userId)
        lessonRepository.getByNumber(command.courseId, command.lessonNumber).rename(command)
    }

    fun findAll(command: LessonCommand.Query): List<LessonDto> {
        courseRepository.getOwned(command.courseId, command.userId)
        return find(command.courseId)
    }

    private fun find(courseId: Long): List<LessonDto> {
        val classrooms = classroomRepository.findAllByCourseIdOrderByNameAsc(courseId)
        val openings =
            lessonOpeningRepository
                .findAllByClassroomIdIn(classrooms.map { requireNotNull(it.id) })
                .associateBy { requireNotNull(it.classroom.id) to it.lessonNumber }
        val counts = questionRepository.findQuestionCounts(courseId).associateBy { it.lessonNumber }
        return lessonRepository.findAllByCourseIdOrderByLessonNumberAsc(courseId).map { lesson ->
            LessonDto(
                lessonNumber = lesson.lessonNumber,
                title = lesson.title,
                questionCount = counts[lesson.lessonNumber]?.questionCount ?: 0,
                classrooms =
                classrooms.map { classroom ->
                    val classroomId = requireNotNull(classroom.id)
                    LessonClassroomStatusDto(
                        classroomId = classroomId,
                        classroomName = classroom.name,
                        status = openings[classroomId to lesson.lessonNumber]?.status ?: LessonStatus.PLANNED,
                    )
                },
            )
        }
    }
}
