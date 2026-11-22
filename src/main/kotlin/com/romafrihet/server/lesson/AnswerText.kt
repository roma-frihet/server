package com.romafrihet.server.lesson

/** 주관식 답을 비교할 때 쓰는 형태다. 채점, 분포 집계, 일괄 인정이 모두 같은 규칙을 써야 결과가 어긋나지 않는다. */
fun normalizeAnswer(value: String): String = value.filterNot(Char::isWhitespace).lowercase()
