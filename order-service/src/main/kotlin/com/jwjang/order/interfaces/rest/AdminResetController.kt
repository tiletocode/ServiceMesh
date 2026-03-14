package com.jwjang.order.interfaces.rest

import com.jwjang.order.infrastructure.persistence.OrderJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminResetController(
    private val orderJpaRepository: OrderJpaRepository
) {
    @PostMapping("/reset")
    @Transactional
    fun reset(): ResponseEntity<Map<String, Any>> {
        val count = orderJpaRepository.count()
        orderJpaRepository.deleteAll()
        return ResponseEntity.ok(mapOf("service" to "order-service", "deleted" to count))
    }
}
