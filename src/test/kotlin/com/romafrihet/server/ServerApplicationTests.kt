package com.romafrihet.server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["app.security.supabase-url=http://localhost:54321"])
@Testcontainers(disabledWithoutDocker = true)
class ServerApplicationTests {
    @Test
    fun contextLoads() = Unit
}
