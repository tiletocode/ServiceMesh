package com.jwjang.member.interfaces.rest

import com.jwjang.member.infrastructure.persistence.MemberJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminResetController(
    private val memberJpaRepository: MemberJpaRepository
) {
    @PostMapping("/reset")
    @Transactional
    fun reset(): ResponseEntity<Map<String, Any>> {
        val count = memberJpaRepository.count()
        memberJpaRepository.deleteAll()
        return ResponseEntity.ok(mapOf("service" to "member-service", "deleted" to count))
    }
}
