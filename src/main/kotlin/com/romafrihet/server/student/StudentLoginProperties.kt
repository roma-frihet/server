package com.romafrihet.server.student

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.student")
data class StudentLoginProperties(
    /** 학번을 이메일 모양으로 바꿀 때 붙이는 도메인이다. 메일을 받지 않으므로 실제로 존재할 필요는 없다. */
    val loginEmailDomain: String = "students.roma-frihet.app",
)
