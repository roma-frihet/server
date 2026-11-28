package com.romafrihet.server.teacher

enum class TeacherApplicationStatus {
    /** 관리자 확인을 기다린다. */
    PENDING,

    /** 교사 역할을 받았다. */
    APPROVED,

    /** 관리자가 거절했다. 다시 신청할 수 있다. */
    REJECTED,

    /** 승인했다가 관리자가 권한을 거뒀다. 스스로 다시 신청할 수 없고 관리자가 다시 승인해야 한다. */
    REVOKED,
}
