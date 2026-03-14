package com.jwjang.order.infrastructure.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * Product Service Feign Client
 * Istio 환경에서는 서비스 디스커버리 대신 k8s Service DNS를 사용할 수도 있음
 */
@FeignClient(name = "product-service", url = "\${service.product.url:}")
interface ProductServiceClient {

    @GetMapping("/api/v1/products/skus/{skuId}")
    fun getSku(@PathVariable skuId: Long): SkuResponse

    @PostMapping("/api/v1/products/skus/{skuId}/deduct")
    fun deductStock(@PathVariable skuId: Long, @RequestBody request: StockRequest): ResponseEntity<Unit>

    @PostMapping("/api/v1/products/skus/{skuId}/restore")
    fun restoreStock(@PathVariable skuId: Long, @RequestBody request: StockRequest): ResponseEntity<Unit>
}

data class SkuResponse(
    val id: Long,
    val skuCode: String,
    val optionName: String,
    val stockQuantity: Int,
    val salesPrice: BigDecimal? = null,
    val productName: String? = null
)

data class StockRequest(val quantity: Int)
