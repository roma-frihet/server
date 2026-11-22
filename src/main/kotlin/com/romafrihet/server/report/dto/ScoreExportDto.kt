package com.romafrihet.server.report.dto

/** 내려받을 파일이다. content는 BOM을 포함한 UTF-8 CSV다. */
class ScoreExportDto(val fileName: String, val content: ByteArray)
