package com.romafrihet.server.common.web

/** URL 경로 상수. 리터럴 경로 하드코딩 금지. */
object ApiPaths {
    const val API_V1 = "/api/v1"
    const val ADMIN_V1 = "$API_V1/admin"
    const val TEACHER_V1 = "$API_V1/teacher"
    const val SCHOOLS_V1 = "$API_V1/schools"
}
