package com.romafrihet.server.student

import com.romafrihet.server.common.exception.ErrorCode
import com.romafrihet.server.common.exception.assertCheck
import com.romafrihet.server.school.SchoolRepository
import com.romafrihet.server.school.getOrThrow
import com.romafrihet.server.student.command.StudentProfileCommand
import com.romafrihet.server.student.dto.StudentProfileDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class StudentProfileService(
    private val studentProfileRepository: StudentProfileRepository,
    private val schoolRepository: SchoolRepository,
) {
    @Transactional
    fun create(command: StudentProfileCommand.Create): StudentProfileDto {
        assertCheck(!studentProfileRepository.existsByUserId(command.userId)) {
            ErrorCode.STUDENT_PROFILE_ALREADY_EXISTS
        }
        assertCheck(
            !studentProfileRepository.existsBySchoolIdAndGradeAndClassNumberAndSeatNumber(
                command.schoolId,
                command.grade,
                command.classNumber,
                command.seatNumber,
            ),
        ) { ErrorCode.STUDENT_NUMBER_DUPLICATED }
        val school = schoolRepository.getOrThrow(command.schoolId)
        return toDto(studentProfileRepository.save(StudentProfile.create(school, command)))
    }

    fun find(userId: UUID): StudentProfileDto? = studentProfileRepository.findByUserId(userId)?.let(::toDto)

    private fun toDto(profile: StudentProfile) = StudentProfileDto(
        schoolId = requireNotNull(profile.school.id),
        schoolName = profile.school.name,
        grade = profile.grade,
        classNumber = profile.classNumber,
        seatNumber = profile.seatNumber,
        studentNumber = profile.studentNumber(),
        name = profile.name,
    )
}
