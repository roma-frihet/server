package com.romafrihet.server.lesson

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.attempt")
data class AttemptProperties(
    /** 마감 직후 도착한 제출까지 인정하는 여유다. 네트워크 지연으로 억울하게 놓치지 않게 한다. */
    val graceSeconds: Long = 2,
)
