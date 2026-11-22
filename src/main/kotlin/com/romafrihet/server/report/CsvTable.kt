package com.romafrihet.server.report

/**
 * 엑셀에서 바로 여는 CSV다. 앞에 BOM을 붙여야 엑셀이 UTF-8로 읽어 한글이 깨지지 않는다.
 * 학생 이름처럼 사용자가 넣은 값이 들어가므로, 수식으로 해석될 수 있는 값은 앞에 '를 붙여 글자로 만든다.
 */
class CsvTable(private val header: List<String>, private val rows: List<List<String>>) {
    fun toBytes(): ByteArray {
        val body = (listOf(header) + rows).joinToString(separator = "\r\n", postfix = "\r\n") { row ->
            row.joinToString(",") { escape(it) }
        }
        return BOM + body.toByteArray(Charsets.UTF_8)
    }

    private fun escape(value: String): String {
        val safe = if (value.isNotEmpty() && value.first() in FORMULA_PREFIXES) "'$value" else value
        val needsQuote = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) "\"${safe.replace("\"", "\"\"")}\"" else safe
    }

    private companion object {
        val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val FORMULA_PREFIXES = setOf('=', '+', '-', '@', '\t', '\r')
    }
}
