package com.romafrihet.server.teacher

import com.romafrihet.server.classroom.ClassroomMembershipRepository
import com.romafrihet.server.classroom.ClassroomRepository
import com.romafrihet.server.classroom.getOwned
import com.romafrihet.server.course.CourseRepository
import com.romafrihet.server.course.getOwned
import com.romafrihet.server.lesson.AttemptCleaner
import com.romafrihet.server.lesson.LessonOpeningRepository
import com.romafrihet.server.lesson.LessonRepository
import com.romafrihet.server.lesson.QuestionCleaner
import com.romafrihet.server.lesson.command.AttemptCommand
import com.romafrihet.server.lesson.command.QuestionCommand
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
    private val questionCleaner: QuestionCleaner,
    private val attemptCleaner: AttemptCleaner,
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
        val courses = courseRepository.findAllByUserIdOrderByGradeAscSubjectDisplayOrderAsc(userId)
        val classrooms =
            classroomRepository
                .findAllByCourseUserIdOrderByCourseIdAscClassNoAsc(userId)
                .groupBy { requireNotNull(it.course.id) }
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
                    subjectId = requireNotNull(course.subject.id),
                    subjectName = course.subject.name,
                    lessonCount = lessonCounts[courseId] ?: 0,
                    classrooms =
                    classrooms[courseId].orEmpty().map {
                        MyPageClassroomDto(id = requireNotNull(it.id), classNo = it.classNo)
                    },
                )
            },
        )
    }

    /**
     * 반을 지우면 그 반의 기록도 함께 사라진다.
     *
     * 전에는 기록이 하나라도 있으면 막았는데, 차시를 한 번 열면 연 기록이 남고 닫아도
     * 지워지지 않아 그 반을 영영 지울 수 없었다. 잘못 만든 반을 되돌릴 방법이 없다는 뜻이라
     * 교사가 직접 지울 수 있게 열었다. 무엇이 함께 사라지는지는 화면에서 확인을 받는다.
     */
    @Transactional
    fun deleteClassroom(command: TeacherCommand.DeleteClassroom) {
        val classroom = classroomRepository.getOwned(command.classroomId, command.userId)
        deleteClassrooms(listOf(command.classroomId))
        classroomRepository.delete(classroom)
    }

    @Transactional
    fun deleteCourse(command: TeacherCommand.DeleteCourse) {
        val course = courseRepository.getOwned(command.courseId, command.userId)
        val classrooms = classroomRepository.findAllByCourseIdOrderByClassNoAsc(command.courseId)
        deleteClassrooms(classrooms.mapNotNull { it.id })
        classroomRepository.deleteAll(classrooms)
        questionCleaner.deleteAllInCourse(QuestionCommand.DeleteAllInCourse(command.courseId))
        lessonRepository.deleteAllByCourseId(command.courseId)
        courseRepository.delete(course)
    }

    /** 외래 키 순서대로 응시 → 연결 → 연 기록을 지운다. 반 자체는 부르는 쪽에서 지운다. */
    private fun deleteClassrooms(classroomIds: List<Long>) {
        if (classroomIds.isEmpty()) return
        attemptCleaner.deleteAllInClassrooms(AttemptCommand.DeleteAllInClassrooms(classroomIds))
        classroomMembershipRepository.deleteAllByClassroomIdIn(classroomIds)
        lessonOpeningRepository.deleteAllByClassroomIdIn(classroomIds)
    }
}
