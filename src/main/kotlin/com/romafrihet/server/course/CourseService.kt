package com.romafrihet.server.course

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.course.command.CourseCommand
import com.romafrihet.server.course.dto.CourseDto
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.school.getOrThrow
import com.romafrihet.server.subject.SubjectRepository
import com.romafrihet.server.subject.getOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CourseService(
    private val courseRepository: CourseRepository,
    private val schoolRepository: SchoolRepository,
    private val subjectRepository: SubjectRepository,
) {
    @Transactional
    fun create(command: CourseCommand.Create): CourseDto {
        assertCheck(
            !courseRepository.existsByUserIdAndGradeAndSubjectId(command.userId, command.grade, command.subjectId),
        ) { ErrorCode.COURSE_DUPLICATED }
        val school = schoolRepository.getOrThrow(command.schoolId)
        val subject = subjectRepository.getOrThrow(command.subjectId)
        return toDto(courseRepository.save(Course.create(school, subject, command)))
    }

    fun findAll(userId: UUID): List<CourseDto> =
        courseRepository.findAllByUserIdOrderByGradeAscSubjectDisplayOrderAsc(userId).map(::toDto)

    private fun toDto(course: Course) = CourseDto(
        id = requireNotNull(course.id),
        schoolId = requireNotNull(course.school.id),
        schoolName = course.school.name,
        grade = course.grade,
        subjectId = requireNotNull(course.subject.id),
        subjectName = course.subject.name,
    )
}
