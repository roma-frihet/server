package com.romafrihet.server.lesson

import com.romafrihet.server.classroom.event.ClassroomMembershipRemovedEvent
import com.romafrihet.server.lesson.command.SubmissionCommand
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/** 내보내기와 같은 트랜잭션에서 지운다. 기록만 남고 학생은 사라지는 상태를 만들지 않는다. */
@Component
class ClassroomMembershipRemovedListener(private val lessonSubmissionService: LessonSubmissionService) {
    @EventListener
    fun handle(event: ClassroomMembershipRemovedEvent) {
        lessonSubmissionService.deleteAllInClassroom(
            SubmissionCommand.DeleteAllInClassroom(classroomId = event.classroomId, userId = event.userId),
        )
    }
}
