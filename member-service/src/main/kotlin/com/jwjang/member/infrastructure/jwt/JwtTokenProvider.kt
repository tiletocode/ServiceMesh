package com.jwjang.member.infrastructure.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

/**
 * JWT 토큰 생성 및 검증 컴포넌트
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long
) {
    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(memberId: Long, email: String, grade: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(memberId.toString())
            .claim("email", email)
            .claim("grade", grade)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return runCatching { getClaims(token) }.isSuccess
    }

    fun getMemberIdFromToken(token: String): Long {
        return getClaims(token).subject.toLong()
    }

    fun getEmailFromToken(token: String): String {
        return getClaims(token)["email", String::class.java]
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
