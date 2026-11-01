package com.romafrihet.server.classroom

import com.romafrihet.server.TeacherFlowFixture
import com.romafrihet.server.classroom.command.ClassroomCommand
import com.romafrihet.server.classroom.command.ClassroomMembershipCommand
import com.romafrihet.server.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ClassroomTeacherFlowTest : TeacherFlowFixture() {
    @Test
    fun `학교는 이름 일부로 찾는다`() {
        assertThat(schoolService.findAll("다원").map { it.name }).containsExactly("다원중학교")
        assertThat(schoolService.findAll("없는학교")).isEmpty()
    }

    @Test
    fun `코드는 수업과 반마다 따로 발급한다`() {
        val first = classroomService.create(ClassroomCommand.Create(teacherId, createCourse("수학"), "1반")).id
        val second = classroomService.create(ClassroomCommand.Create(teacherId, createCourse("국어"), "1반")).id

        val firstCode = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, first)).joinCode
        val secondCode = classroomService.issueJoinCode(ClassroomCommand.IssueJoinCode(teacherId, second)).joinCode

        assertThat(firstCode).isNotEqualTo(secondCode)
    }

    @Test
    fun `코드를 끄면 비워지고 일괄 승인하면 연결 학생 수가 늘어난다`() {
        val classroomId = createClassroom()
        val issue = ClassroomCommand.IssueJoinCode(teacherId, classroomId)
        classroomService.issueJoinCode(issue)
        request(classroomId, studentId, "홍길동", "20101")

        assertThat(membershipService.approveAll(ClassroomMembershipCommand.ApproveAll(teacherId, classroomId)))
            .hasSize(1)
        assertThat(classroomService.findAll(teacherId).single().studentCount).isEqualTo(1)

        classroomService.closeJoinCode(issue)
        assertThat(classroomService.findAll(teacherId).single().joinCode).isNull()
    }

    @Test
    fun `승인과 거절은 대기 요청에만 하고 다른 교사는 할 수 없다`() {
        val classroomId = createClassroom()
        val id = requireNotNull(request(classroomId, studentId, "홍길동", "20101").id)

        assertErrorCode(ErrorCode.NOT_FOUND) {
            membershipService.approve(ClassroomMembershipCommand.Decide(UUID.randomUUID(), classroomId, id))
        }
        membershipService.reject(ClassroomMembershipCommand.Decide(teacherId, classroomId, id))

        assertErrorCode(ErrorCode.MEMBERSHIP_NOT_PENDING) {
            membershipService.approve(ClassroomMembershipCommand.Decide(teacherId, classroomId, id))
        }
    }
}
