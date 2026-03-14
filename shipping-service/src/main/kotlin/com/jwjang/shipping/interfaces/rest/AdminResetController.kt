package com.jwjang.shipping.interfaces.rest

import com.jwjang.shipping.infrastructure.persistence.ShipmentJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminResetController(
    private val shipmentJpaRepository: ShipmentJpaRepository
) {
    @PostMapping("/reset")
    @Transactional
    fun reset(): ResponseEntity<Map<String, Any>> {
        val count = shipmentJpaRepository.count()
        shipmentJpaRepository.deleteAll()
        return ResponseEntity.ok(mapOf("service" to "shipping-service", "deleted" to count))
    }
}
