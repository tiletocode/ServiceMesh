package com.jwjang.product.interfaces.rest

import com.jwjang.product.infrastructure.persistence.ProductJpaRepository
import com.jwjang.product.infrastructure.persistence.SkuJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminResetController(
    private val skuJpaRepository: SkuJpaRepository,
    private val productJpaRepository: ProductJpaRepository
) {
    @PostMapping("/reset")
    @Transactional
    fun reset(): ResponseEntity<Map<String, Any>> {
        val skuCount = skuJpaRepository.count()
        val productCount = productJpaRepository.count()
        skuJpaRepository.deleteAll()
        productJpaRepository.deleteAll()
        return ResponseEntity.ok(mapOf(
            "service" to "product-service",
            "deletedSkus" to skuCount,
            "deletedProducts" to productCount
        ))
    }
}
