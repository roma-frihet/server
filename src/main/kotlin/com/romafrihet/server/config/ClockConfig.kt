package com.romafrihet.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** 만료와 마감 판단은 서버 시계를 기준으로 한다. 테스트에서 시간을 앞으로 돌리려고 빈으로 분리한다. */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
