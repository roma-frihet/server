package com.romafrihet.server.classroom

import org.springframework.stereotype.Component
import java.security.SecureRandom

/** 칠판에 적어 불러 주는 코드라 0/O, 1/I/L처럼 헷갈리는 글자를 뺀다. */
@Component
class JoinCodeGenerator {
    private val random = SecureRandom()

    fun generate(): String = String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    private companion object {
        const val LENGTH = 6
        const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
