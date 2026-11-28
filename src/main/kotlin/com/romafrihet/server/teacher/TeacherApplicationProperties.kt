package com.romafrihet.server.teacher

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.teacher-application")
data class TeacherApplicationProperties(
    /**
     * 켜면 이메일 인증([TeacherApplication.emailVerifiedAt])을 마친 신청만 승인한다. 인증 기능을 붙일 때 켠다.
     * Supabase의 Confirm email은 학생이 받을 수 없는 주소로 가입해서 켤 수 없으므로 여기서 따로 막는다.
     */
    val requireEmailVerified: Boolean = false,
)
