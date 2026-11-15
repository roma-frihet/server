package com.romafrihet.server.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.supabase-auth")
data class SupabaseAuthProperties(
    val supabaseUrl: String,
    /** 서버 전용 service_role 키. .env로만 관리한다. */
    val serviceRoleKey: String = "",
)
