package com.romafrihet.server.admin

import com.romafrihet.server.admin.command.AdminCommand
import com.romafrihet.server.admin.dto.AdminQuestionDto
import com.romafrihet.server.lesson.QuestionRepository
import com.romafrihet.server.lesson.dto.QuestionAdminQueryDto
import com.romafrihet.server.teacher.TeacherProfileRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 모든 교사의 문제를 본다. 교사 이름은 교사 기능에 있어 여기서 붙인다.
 * 차시·문제 기능이 교사 기능에 기대면 순환이 생기므로 관리자 기능이 둘을 엮는다.
 */
@Service
@Transactional(readOnly = true)
class QuestionAdminService(
    private val questionRepository: QuestionRepository,
    private val teacherProfileRepository: TeacherProfileRepository,
) {
    fun findAll(command: AdminCommand.QuestionQuery): Page<AdminQuestionDto> {
        val page =
            questionRepository.queryForAdmin(
                QuestionAdminQueryDto(
                    schoolId = command.schoolId,
                    subjectId = command.subjectId,
                    teacherUserId = command.teacherUserId,
                    type = command.type,
                    keyword = command.keyword,
                ),
                PageRequest.of(command.page, command.size),
            )
        val names =
            teacherProfileRepository
                .findAllByUserIdIn(page.content.map { it.teacherUserId }.distinct())
                .associate { it.userId to it.name }
        return page.map {
            AdminQuestionDto(
                questionId = it.questionId,
                courseId = it.courseId,
                teacherUserId = it.teacherUserId,
                teacherName = names[it.teacherUserId],
                schoolName = it.schoolName,
                grade = it.grade,
                subjectName = it.subjectName,
                lessonNumber = it.lessonNumber,
                lessonTitle = it.lessonTitle,
                type = it.type,
                content = it.content,
                hasImage = it.imageId != null,
                createdAt = it.createdAt,
            )
        }
    }
}
