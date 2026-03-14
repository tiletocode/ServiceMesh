package com.jwjang.member.application.service

import com.jwjang.member.application.command.*
import com.jwjang.member.application.query.LoginResultView
import com.jwjang.member.application.query.MemberView
import com.jwjang.member.domain.exception.*
import com.jwjang.member.domain.model.Member
import com.jwjang.member.domain.repository.MemberRepository
import com.jwjang.member.infrastructure.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 애플리케이션 서비스
 * Use Case를 조율하며 도메인 로직은 도메인 모델에 위임
 */
@Service
@Transactional
class MemberApplicationService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 회원 가입
     */
    fun register(command: RegisterMemberCommand): MemberView {
        if (memberRepository.existsByEmail(command.email)) {
            throw MemberEmailAlreadyExistsException(command.email)
        }

        val member = Member(
            email = command.email,
            password = passwordEncoder.encode(command.password),
            name = command.name
        )

        return MemberView.from(memberRepository.save(member))
    }

    /**
     * 로그인 (JWT 발급)
     */
    fun login(command: LoginCommand): LoginResultView {
        val member = memberRepository.findByEmail(command.email)
            .orElseThrow { InvalidCredentialsException() }

        if (!member.isActive) throw MemberWithdrawnException()

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw InvalidCredentialsException()
        }

        val token = jwtTokenProvider.generateToken(member.id!!, member.email, member.grade.name)

        return LoginResultView(
            accessToken = token,
            memberId = member.id,
            email = member.email,
            name = member.name,
            grade = member.grade
        )
    }

    /**
     * 회원 프로필 조회
     */
    @Transactional(readOnly = true)
    fun getMember(memberId: Long): MemberView {
        val member = memberRepository.findById(memberId)
            .orElseThrow { MemberNotFoundException(memberId) }
        return MemberView.from(member)
    }

    /**
     * 회원 프로필 수정
     */
    fun updateProfile(command: UpdateMemberProfileCommand): MemberView {
        val member = memberRepository.findById(command.memberId)
            .orElseThrow { MemberNotFoundException(command.memberId) }

        member.updateProfile(command.name)
        return MemberView.from(memberRepository.save(member))
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(command: ChangeMemberPasswordCommand) {
        val member = memberRepository.findById(command.memberId)
            .orElseThrow { MemberNotFoundException(command.memberId) }

        if (!passwordEncoder.matches(command.currentPassword, member.password)) {
            throw InvalidCredentialsException()
        }

        member.changePassword(passwordEncoder.encode(command.newPassword))
        memberRepository.save(member)
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     */
    fun withdraw(memberId: Long) {
        val member = memberRepository.findById(memberId)
            .orElseThrow { MemberNotFoundException(memberId) }

        member.withdraw()
        memberRepository.save(member)
    }
}
