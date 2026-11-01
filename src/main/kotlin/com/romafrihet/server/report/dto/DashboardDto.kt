package com.romafrihet.server.report.dto

import com.romafrihet.server.lesson.LessonStatus

data class DashboardDto(
    val classroomCount: Int,
    val openLessonCount: Int,
    val averageCorrect: Double?,
    val averageQuestionCount: Double?,
    val setup: DashboardSetupDto,
    val liveLessons: List<LiveLessonDto>,
    val lessons: List<DashboardLessonDto>,
    val classrooms: List<DashboardClassroomDto>,
    val droppedStudents: List<WatchStudentDto>,
    val notRespondedStudents: List<WatchStudentDto>,
)

data class DashboardSetupDto(
    val hasClassroom: Boolean,
    val hasLesson: Boolean,
    val hasQuestion: Boolean,
    val hasOpenedLesson: Boolean,
)

data class LiveLessonDto(
    val classroomId: Long,
    val classroomName: String,
    val lessonNumber: Int,
    val lessonTitle: String,
    val responseCount: Int,
    val studentCount: Int,
    val averageCorrect: Double?,
    val questionCount: Int,
)

data class DashboardLessonDto(val lessonNumber: Int, val title: String)

data class DashboardClassroomDto(
    val classroomId: Long,
    val classroomName: String,
    val latestAverageCorrect: Double?,
    val latestQuestionCount: Int,
    val lessonStatuses: List<DashboardLessonStatusDto>,
)

data class DashboardLessonStatusDto(val lessonNumber: Int, val status: LessonStatus)

data class WatchStudentDto(
    val classroomId: Long,
    val classroomName: String,
    val membershipId: Long,
    val studentName: String,
    val studentNumber: String,
    val previousCorrectCount: Int?,
    val latestCorrectCount: Int?,
)
