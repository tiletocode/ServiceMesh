package com.jwjang.member.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 회원 Aggregate Root
 * 우리 시스템에 가입하여 고유 ID를 발급받은 개인
 */
@Entity
@Table(
    name = "members",
    indexes = [Index(name = "idx_members_email", columnList = "email", unique = true)]
)
class Member(
    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(nullable = false, length = 255)
    var password: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var grade: MemberGrade = MemberGrade.BRONZE,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        email = "",
        password = "",
        name = ""
    )

    /**
     * 회원 정보 수정 (도메인 규칙 적용)
     */
    fun updateProfile(name: String) {
        require(name.isNotBlank()) { "이름은 공백일 수 없습니다." }
        this.name = name
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(encodedPassword: String) {
        require(encodedPassword.isNotBlank()) { "비밀번호는 공백일 수 없습니다." }
        this.password = encodedPassword
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 회원 등급 업그레이드
     */
    fun upgradeGrade(newGrade: MemberGrade) {
        require(newGrade.ordinal >= this.grade.ordinal) {
            "등급은 현재 등급(${this.grade})보다 낮출 수 없습니다."
        }
        this.grade = newGrade
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     */
    fun withdraw() {
        check(isActive) { "이미 탈퇴한 회원입니다." }
        this.isActive = false
        this.updatedAt = LocalDateTime.now()
    }
}
