package com.romafrihet.server.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/** 권한은 사용자가 수정할 수 없는 app_metadata로만 판단한다. */
@Component
class SupabaseJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken = JwtAuthenticationToken(jwt, authoritiesOf(jwt))

    private fun authoritiesOf(jwt: Jwt): List<GrantedAuthority> {
        val authority =
            when (jwt.getClaimAsMap(APP_METADATA_CLAIM)?.get(ROLE_KEY)) {
                TEACHER_ROLE -> ROLE_TEACHER
                ADMIN_ROLE -> ROLE_ADMIN
                else -> null
            }
        return listOfNotNull(authority?.let(::SimpleGrantedAuthority))
    }

    private companion object {
        const val APP_METADATA_CLAIM = "app_metadata"
        const val ROLE_KEY = "role"
        const val TEACHER_ROLE = "teacher"
        const val ROLE_TEACHER = "ROLE_TEACHER"

        /** 관리자는 교사 API를 쓰지 않는다. 교사 화면은 자기 수업만 다루므로 관리자에게 열 이유가 없다. */
        const val ADMIN_ROLE = "admin"
        const val ROLE_ADMIN = "ROLE_ADMIN"
    }
}
