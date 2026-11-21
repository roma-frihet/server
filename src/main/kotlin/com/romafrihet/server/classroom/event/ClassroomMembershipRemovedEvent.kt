package com.romafrihet.server.classroom.event

import java.util.UUID

/** 교사가 학생을 반에서 내보냈다. 그 반에서 남긴 기록도 지워야 한다. */
data class ClassroomMembershipRemovedEvent(val classroomId: Long, val userId: UUID)
