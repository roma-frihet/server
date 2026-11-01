package com.romafrihet.server.teacher.rqrs

import com.romafrihet.server.teacher.dto.MyPageClassroomDto
import com.romafrihet.server.teacher.dto.MyPageCourseDto
import com.romafrihet.server.teacher.dto.MyPageDto
import com.romafrihet.server.teacher.dto.MyPageProfileDto
import io.swagger.v3.oas.annotations.media.Schema

class TeacherResponse private constructor() {
    @Schema(name = "TeacherResponseClassroom")
    data class Classroom(val id: Long, val name: String) {
        companion object {
            fun of(dto: MyPageClassroomDto) = Classroom(id = dto.id, name = dto.name)
        }
    }

    @Schema(name = "TeacherResponseCourse")
    data class Course(
        val id: Long,
        val grade: Int,
        val subject: String,
        val lessonCount: Int,
        val classrooms: List<Classroom>,
    ) {
        companion object {
            fun of(dto: MyPageCourseDto) = Course(
                id = dto.id,
                grade = dto.grade,
                subject = dto.subject,
                lessonCount = dto.lessonCount,
                classrooms = dto.classrooms.map(Classroom::of),
            )
        }
    }

    @Schema(name = "TeacherResponseProfile")
    data class Profile(val name: String, val schoolId: Long, val schoolName: String) {
        companion object {
            fun of(dto: MyPageProfileDto) = Profile(
                name = dto.name,
                schoolId = dto.schoolId,
                schoolName = dto.schoolName,
            )
        }
    }

    @Schema(name = "TeacherResponseMyPage")
    data class MyPage(@Schema(nullable = true) val profile: Profile?, val courses: List<Course>) {
        companion object {
            fun of(dto: MyPageDto) = MyPage(
                profile = dto.profile?.let(Profile::of),
                courses = dto.courses.map(Course::of),
            )
        }
    }
}
