package com.jwjang.member.domain.exception

class MemberNotFoundException(id: Long) :
    RuntimeException("회원을 찾을 수 없습니다. id=$id")

class MemberEmailAlreadyExistsException(email: String) :
    RuntimeException("이미 사용 중인 이메일입니다. email=$email")

class MemberWithdrawnException :
    RuntimeException("탈퇴한 회원입니다.")

class InvalidCredentialsException :
    RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.")
