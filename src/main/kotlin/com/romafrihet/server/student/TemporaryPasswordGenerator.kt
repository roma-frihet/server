package com.romafrihet.server.student

import org.springframework.stereotype.Component
import java.security.SecureRandom

/** 교사가 학생에게 불러 주는 임시 비밀번호라 헷갈리는 글자를 뺀다. */
@Component
class TemporaryPasswordGenerator {
    private val random = SecureRandom()

    fun generate(): String = String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    private companion object {
        const val LENGTH = 8
        const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
    }
}
