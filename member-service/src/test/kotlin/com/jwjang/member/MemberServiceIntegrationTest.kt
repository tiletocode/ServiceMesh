package com.jwjang.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

/**
 * member-service 통합 테스트
 * H2 인메모리 DB + Spring Security + JWT 전체 흐름 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MemberServiceIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        var savedMemberId: Long = 0
        var jwtToken: String = ""
        const val EMAIL = "integration@test.com"
        const val PASSWORD = "testpass123"
    }

    @Test
    @Order(1)
    fun `회원 가입 성공 - 201 반환`() {
        val request = mapOf(
            "email" to EMAIL,
            "password" to PASSWORD,
            "name" to "통합테스트사용자"
        )
        val response = restTemplate.postForEntity(
            "/api/v1/members/register",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body!!["email"]).isEqualTo(EMAIL)
        assertThat(response.body!!["name"]).isEqualTo("통합테스트사용자")
        assertThat(response.body!!["grade"]).isEqualTo("BRONZE")
        savedMemberId = (response.body!!["id"] as Int).toLong()
    }

    @Test
    @Order(2)
    fun `중복 이메일 가입 시 409 CONFLICT 반환`() {
        val request = mapOf(
            "email" to EMAIL,
            "password" to PASSWORD,
            "name" to "중복사용자"
        )
        val response = restTemplate.postForEntity(
            "/api/v1/members/register",
            request,
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @Order(3)
    fun `로그인 성공 - JWT 토큰 반환`() {
        val request = mapOf("email" to EMAIL, "password" to PASSWORD)
        val response = restTemplate.postForEntity(
            "/api/v1/members/login",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["accessToken"]).isNotNull()
        assertThat(body["tokenType"]).isEqualTo("Bearer")
        assertThat(body["email"]).isEqualTo(EMAIL)
        jwtToken = body["accessToken"] as String
    }

    @Test
    @Order(4)
    fun `잘못된 비밀번호 로그인 시 401 UNAUTHORIZED 반환`() {
        val request = mapOf("email" to EMAIL, "password" to "wrongpassword!")
        val response = restTemplate.postForEntity(
            "/api/v1/members/login",
            request,
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @Order(5)
    fun `JWT 없이 회원 조회 시 403 반환`() {
        val response = restTemplate.getForEntity(
            "/api/v1/members/$savedMemberId",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @Order(6)
    fun `JWT 인증 후 회원 조회 성공`() {
        val headers = HttpHeaders().apply { set("Authorization", "Bearer $jwtToken") }
        val entity = HttpEntity<Any>(headers)

        val response = restTemplate.exchange(
            "/api/v1/members/$savedMemberId",
            HttpMethod.GET,
            entity,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["email"]).isEqualTo(EMAIL)
        assertThat(response.body!!["isActive"]).isEqualTo(true)
    }

    @Test
    @Order(7)
    fun `회원 프로필 수정 성공`() {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $jwtToken")
            contentType = MediaType.APPLICATION_JSON
        }
        val request = mapOf("name" to "수정된이름")
        val response = restTemplate.exchange(
            "/api/v1/members/$savedMemberId/profile",
            HttpMethod.PUT,
            HttpEntity(request, headers),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["name"]).isEqualTo("수정된이름")
    }

    @Test
    @Order(8)
    fun `비밀번호 변경 성공`() {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $jwtToken")
            contentType = MediaType.APPLICATION_JSON
        }
        val request = mapOf(
            "currentPassword" to PASSWORD,
            "newPassword" to "newpass456"
        )
        val response = restTemplate.exchange(
            "/api/v1/members/$savedMemberId/password",
            HttpMethod.PUT,
            HttpEntity(request, headers),
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    @Order(9)
    fun `회원 탈퇴 성공`() {
        val headers = HttpHeaders().apply { set("Authorization", "Bearer $jwtToken") }
        val response = restTemplate.exchange(
            "/api/v1/members/$savedMemberId",
            HttpMethod.DELETE,
            HttpEntity<Any>(headers),
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
