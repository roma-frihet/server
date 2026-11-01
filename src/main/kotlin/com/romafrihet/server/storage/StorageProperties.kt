package com.romafrihet.server.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.storage")
data class StorageProperties(
    val supabaseUrl: String,
    /** 서버 전용 service_role 키. .env로만 관리한다. */
    val serviceRoleKey: String = "",
    val bucket: String,
    val signedUrlTtlSeconds: Long,
)
