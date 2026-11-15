package com.romafrihet.server.lesson

/** 문제 하나의 확정된 결과다. 시작하지 않은 문제는 행이 없고 미응답으로 센다. */
enum class QuestionResult {
    RIGHT,
    WRONG,
    NONE,
}
