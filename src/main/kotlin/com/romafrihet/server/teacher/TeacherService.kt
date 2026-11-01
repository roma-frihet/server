package com.romafrihet.server.teacher

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.school.getOrThrow
import com.romafrihet.server.teacher.command.TeacherCommand
import com.romafrihet.server.teacher.dto.MyPageClassroomDto
import com.romafrihet.server.teacher.dto.MyPageCourseDto
import com.romafrihet.server.teacher.dto.MyPageDto
import com.romafrihet.server.teacher.dto.MyPageProfileDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TeacherService(
    private val teacherProfileRepository: TeacherProfileRepository,
    private val schoolRepository: SchoolRepository,
    private val courseRepository: CourseRepository,
    private val classroomRepository: ClassroomRepository,
    private val classroomMembershipRepository: ClassroomMembershipRepository,
    private val lessonRepository: LessonRepository,
    private val lessonOpeningRepository: LessonOpeningRepository,
    private val questionRepository: QuestionRepository,
) {
    @Transactional
    fun saveProfile(command: TeacherCommand.SaveProfile) {
        val school = schoolRepository.getOrThrow(command.schoolId)
        val profile = teacherProfileRepository.findByUserId(command.userId)
        if (profile != null) {
            profile.update(school, command)
        } else {
            teacherProfileRepository.save(TeacherProfile.create(school, command))
        }
    }

    fun findMyPage(userId: UUID): MyPageDto {
        val profile = teacherProfileRepository.findByUserId(userId)
        val courses = courseRepository.findAllByUserIdOrderByGradeAscSubjectAsc(userId)
        val classrooms =
            classroomRepository.findAllByCourseUserIdOrderByNameAsc(userId).groupBy { requireNotNull(it.course.id) }
        val lessonCounts =
            lessonRepository
                .findAllByCourseIdIn(courses.map { requireNotNull(it.id) })
                .groupingBy { requireNotNull(it.course.id) }
                .eachCount()
        return MyPageDto(
            profile =
            profile?.let {
                MyPageProfileDto(name = it.name, schoolId = requireNotNull(it.school.id), schoolName = it.school.name)
            },
            courses =
            courses.map { course ->
                val courseId = requireNotNull(course.id)
                MyPageCourseDto(
                    id = courseId,
                    grade = course.grade,
                    subject = course.subject,
                    lessonCount = lessonCounts[courseId] ?: 0,
                    classrooms =
                    classrooms[courseId].orEmpty().map {
                        MyPageClassroomDto(id = requireNotNull(it.id), name = it.name)
                    },
                )
            },
        )
    }

    /** 학생 기록이 사라지지 않도록 기록이 있는 반과 수업은 지우지 않는다. */
    @Transactional
    fun deleteClassroom(command: TeacherCommand.DeleteClassroom) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        assertCheck(
            !classroomMembershipRepository.existsByClassroomId(command.classroomId) &&
                !lessonOpeningRepository.existsByClassroomId(command.classroomId),
        ) { ErrorCode.CLASSROOM_HAS_RECORDS }
        classroomRepository.delete(classroom)
    }

    @Transactional
    fun deleteCourse(command: TeacherCommand.DeleteCourse) {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        assertCheck(
            !classroomRepository.existsByCourseId(command.courseId) &&
                !questionRepository.existsByCourseId(command.courseId),
        ) { ErrorCode.COURSE_HAS_RECORDS }
        lessonRepository.deleteAllByCourseId(command.courseId)
        courseRepository.delete(course)
    }
}
