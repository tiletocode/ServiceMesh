package com.jwjang.member.application.query

import com.jwjang.member.domain.model.Member
import com.jwjang.member.domain.model.MemberGrade
import java.time.LocalDateTime

/**
 * 회원 정보 뷰 (Query DTO)
 */
data class MemberView(
    val id: Long,
    val email: String,
    val name: String,
    val grade: MemberGrade,
    val isActive: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): MemberView = MemberView(
            id = member.id!!,
            email = member.email,
            name = member.name,
            grade = member.grade,
            isActive = member.isActive,
            createdAt = member.createdAt
        )
    }
}

/**
 * 로그인 응답 뷰
 */
data class LoginResultView(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val memberId: Long,
    val email: String,
    val name: String,
    val grade: MemberGrade
)
