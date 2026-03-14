package com.jwjang.member.infrastructure.security

import com.jwjang.member.infrastructure.jwt.JwtTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터
 * 요청 헤더의 Bearer 토큰을 검증하고 SecurityContext에 인증 정보를 설정
 */
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val memberId = jwtTokenProvider.getMemberIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)

            val auth = UsernamePasswordAuthenticationToken(
                email,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
            )
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization") ?: return null
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}
