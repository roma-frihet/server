package com.romafrihet.server.lesson

/** PLANNED는 반에 아직 열지 않은 차시다. 마감은 되돌릴 수 없다. */
enum class LessonStatus {
    PLANNED,
    OPEN,
    CLOSED,
    FINISHED,
}
