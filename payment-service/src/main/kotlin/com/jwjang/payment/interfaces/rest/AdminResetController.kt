package com.jwjang.payment.interfaces.rest

import com.jwjang.payment.infrastructure.persistence.PaymentJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminResetController(
    private val paymentJpaRepository: PaymentJpaRepository
) {
    @PostMapping("/reset")
    @Transactional
    fun reset(): ResponseEntity<Map<String, Any>> {
        val count = paymentJpaRepository.count()
        paymentJpaRepository.deleteAll()
        return ResponseEntity.ok(mapOf("service" to "payment-service", "deleted" to count))
    }
}
