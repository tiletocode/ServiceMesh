package com.jwjang.member.infrastructure.persistence

import com.jwjang.member.domain.model.Member
import com.jwjang.member.domain.repository.MemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Spring Data JPA Repository 인터페이스 (Infrastructure Layer)
 */
interface MemberJpaRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun existsByEmail(email: String): Boolean
}

/**
 * 도메인 MemberRepository 인터페이스의 JPA 구현체
 */
@Repository
class MemberRepositoryImpl(
    private val jpaRepository: MemberJpaRepository
) : MemberRepository {

    override fun save(member: Member): Member = jpaRepository.save(member)

    override fun findById(id: Long): Optional<Member> = jpaRepository.findById(id)

    override fun findByEmail(email: String): Optional<Member> = jpaRepository.findByEmail(email)

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email)

    override fun delete(member: Member) = jpaRepository.delete(member)
}
