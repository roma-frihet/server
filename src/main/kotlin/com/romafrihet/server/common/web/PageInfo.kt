package com.romafrihet.server.common.web

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

/** 페이지 응답 공통 메타데이터. Spring Page를 그대로 반환하지 않는다. */
@Schema(name = "PageInfo")
class PageInfo(
    @field:Schema(description = "현재 페이지(0부터)") val page: Int,
    @field:Schema(description = "페이지 크기") val size: Int,
    @field:Schema(description = "전체 요소 수") val totalElements: Long,
    @field:Schema(description = "전체 페이지 수") val totalPages: Int,
    @field:Schema(description = "다음 페이지 존재 여부") val hasNext: Boolean,
) {
    companion object {
        fun of(page: Page<*>) = PageInfo(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }
}
