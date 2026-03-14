package com.jwjang.member.application.command

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 회원 가입 커맨드
 */
data class RegisterMemberCommand(
    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    val name: String
)

/**
 * 회원 프로필 수정 커맨드
 */
data class UpdateMemberProfileCommand(
    val memberId: Long,

    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    val name: String
)

/**
 * 비밀번호 변경 커맨드
 */
data class ChangeMemberPasswordCommand(
    val memberId: Long,

    @field:NotBlank(message = "현재 비밀번호는 필수입니다.")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다.")
    val newPassword: String
)

/**
 * 로그인 커맨드
 */
data class LoginCommand(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)
