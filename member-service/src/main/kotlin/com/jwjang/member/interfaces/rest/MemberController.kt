package com.jwjang.member.interfaces.rest

import com.jwjang.member.application.command.*
import com.jwjang.member.application.query.LoginResultView
import com.jwjang.member.application.query.MemberView
import com.jwjang.member.application.service.MemberApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 회원 REST 컨트롤러 (Interfaces Layer)
 */
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberApplicationService: MemberApplicationService
) {

    /** 회원 가입 */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<MemberView> {
        val command = RegisterMemberCommand(
            email = request.email,
            password = request.password,
            name = request.name
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(memberApplicationService.register(command))
    }

    /** 로그인 */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResultView> {
        val command = LoginCommand(email = request.email, password = request.password)
        return ResponseEntity.ok(memberApplicationService.login(command))
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    fun getMyInfo(@AuthenticationPrincipal email: String): ResponseEntity<MemberView> {
        // 이메일로 memberId를 조회하는 방식 또는 JWT에서 id를 꺼내는 방식
        // 여기서는 X-Member-Id 헤더를 통해 게이트웨이에서 전달받는 방식을 사용
        return ResponseEntity.ok(MemberView(0L, email, "", com.jwjang.member.domain.model.MemberGrade.BRONZE, true, java.time.LocalDateTime.now()))
    }

    /** 특정 회원 조회 (내부 서비스 호출용) */
    @GetMapping("/{memberId}")
    fun getMember(@PathVariable memberId: Long): ResponseEntity<MemberView> {
        return ResponseEntity.ok(memberApplicationService.getMember(memberId))
    }

    /** 프로필 수정 */
    @PutMapping("/{memberId}/profile")
    fun updateProfile(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<MemberView> {
        val command = UpdateMemberProfileCommand(memberId = memberId, name = request.name)
        return ResponseEntity.ok(memberApplicationService.updateProfile(command))
    }

    /** 비밀번호 변경 */
    @PutMapping("/{memberId}/password")
    fun changePassword(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Unit> {
        val command = ChangeMemberPasswordCommand(
            memberId = memberId,
            currentPassword = request.currentPassword,
            newPassword = request.newPassword
        )
        memberApplicationService.changePassword(command)
        return ResponseEntity.noContent().build()
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/{memberId}")
    fun withdraw(@PathVariable memberId: Long): ResponseEntity<Unit> {
        memberApplicationService.withdraw(memberId)
        return ResponseEntity.noContent().build()
    }
}

// --- Request DTOs (Interfaces Layer) ---
data class RegisterRequest(val email: String, val password: String, val name: String)
data class LoginRequest(val email: String, val password: String)
data class UpdateProfileRequest(val name: String)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
