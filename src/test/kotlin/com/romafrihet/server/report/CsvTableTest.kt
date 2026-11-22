package com.romafrihet.server.report

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsvTableTest {
    @Test
    fun `엑셀이 UTF-8로 읽도록 BOM을 붙이고 줄은 CRLF로 끝난다`() {
        val bytes = CsvTable(listOf("이름"), listOf(listOf("홍길동"))).toBytes()

        assertThat(bytes.take(3)).containsExactly(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        assertThat(text(bytes)).isEqualTo("이름\r\n홍길동\r\n")
    }

    @Test
    fun `쉼표와 따옴표와 줄바꿈이 든 값은 따옴표로 감싸고 따옴표는 두 번 쓴다`() {
        val csv = text(CsvTable(listOf("내용"), listOf(listOf("가, 나"), listOf("\"인용\""), listOf("두\n줄"))).toBytes())

        assertThat(csv).isEqualTo("내용\r\n\"가, 나\"\r\n\"\"\"인용\"\"\"\r\n\"두\n줄\"\r\n")
    }

    @Test
    fun `수식으로 해석될 수 있는 값은 앞에 작은따옴표를 붙인다`() {
        val csv =
            text(CsvTable(listOf("값"), listOf(listOf("=1+1"), listOf("+82"), listOf("@SUM"), listOf("-3"))).toBytes())

        assertThat(csv.lines().drop(1).filter { it.isNotEmpty() }.map { it.trimEnd('\r') })
            .containsExactly("'=1+1", "'+82", "'@SUM", "'-3")
    }

    private fun text(bytes: ByteArray) = String(bytes.copyOfRange(3, bytes.size), Charsets.UTF_8)
}
