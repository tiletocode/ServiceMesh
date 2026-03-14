package com.jwjang.member.domain.repository

import com.jwjang.member.domain.model.Member
import java.util.Optional

/**
 * 회원 Repository 인터페이스 (Domain Layer)
 * 인프라 구현체와 도메인을 분리하기 위한 포트(Port)
 */
interface MemberRepository {
    fun save(member: Member): Member
    fun findById(id: Long): Optional<Member>
    fun findByEmail(email: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
    fun delete(member: Member)
}
