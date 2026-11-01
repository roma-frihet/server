package com.romafrihet.server.teacher.dto

data class MyPageDto(val name: String, val schoolId: Long, val schoolName: String, val courses: List<MyPageCourseDto>)

data class MyPageCourseDto(
    val id: Long,
    val grade: Int,
    val subject: String,
    val lessonCount: Int,
    val classrooms: List<MyPageClassroomDto>,
)

data class MyPageClassroomDto(val id: Long, val name: String)
