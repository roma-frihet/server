package com.romafrihet.server.lesson

/** READY는 문제 사이다. 이 상태에서는 어떤 시간도 흐르지 않는다. */
enum class AttemptPhase {
    READY,
    IN_QUESTION,
    DONE,
}
