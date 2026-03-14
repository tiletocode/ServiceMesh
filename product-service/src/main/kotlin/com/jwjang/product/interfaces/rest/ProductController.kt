package com.jwjang.product.interfaces.rest

import com.jwjang.product.application.service.*
import com.jwjang.product.domain.model.ProductCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productApplicationService: ProductApplicationService
) {
    /** 상품 목록 조회 (판매 중인 상품, 캐시 적용) */
    @GetMapping
    fun getProducts(@PageableDefault(size = 20) pageable: Pageable): ResponseEntity<Page<ProductView>> {
        return ResponseEntity.ok(productApplicationService.getOnSaleProducts(pageable))
    }

    /** 상품 상세 조회 (캐시 적용) */
    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: Long): ResponseEntity<ProductView> {
        return ResponseEntity.ok(productApplicationService.getProduct(productId))
    }

    /** 상품 등록 */
    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ResponseEntity<ProductView> {
        val command = CreateProductCommand(
            name = request.name,
            description = request.description,
            salesPrice = request.salesPrice,
            category = request.category,
            imageUrl = request.imageUrl
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productApplicationService.createProduct(command))
    }

    /** SKU 추가 */
    @PostMapping("/{productId}/skus")
    fun addSku(
        @PathVariable productId: Long,
        @RequestBody request: AddSkuRequest
    ): ResponseEntity<SkuView> {
        val command = AddSkuCommand(
            productId = productId,
            skuCode = request.skuCode,
            optionName = request.optionName,
            initialStock = request.initialStock
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productApplicationService.addSku(command))
    }

    /** SKU 재고 조회 */
    @GetMapping("/skus/{skuId}")
    fun getSku(@PathVariable skuId: Long): ResponseEntity<SkuView> {
        return ResponseEntity.ok(productApplicationService.getSku(skuId))
    }

    /** 재고 차감 (내부 서비스 호출용 - order-service) */
    @PostMapping("/skus/{skuId}/deduct")
    fun deductStock(
        @PathVariable skuId: Long,
        @RequestBody request: StockRequest
    ): ResponseEntity<Unit> {
        productApplicationService.deductStock(DeductStockCommand(skuId, request.quantity))
        return ResponseEntity.ok().build()
    }

    /** 재고 복구 (주문 취소 시 - SAGA 보상 트랜잭션) */
    @PostMapping("/skus/{skuId}/restore")
    fun restoreStock(
        @PathVariable skuId: Long,
        @RequestBody request: StockRequest
    ): ResponseEntity<Unit> {
        productApplicationService.restoreStock(RestoreStockCommand(skuId, request.quantity))
        return ResponseEntity.ok().build()
    }
}

data class CreateProductRequest(
    val name: String,
    val description: String,
    val salesPrice: BigDecimal,
    val category: ProductCategory,
    val imageUrl: String? = null
)

data class AddSkuRequest(
    val skuCode: String,
    val optionName: String,
    val initialStock: Int = 0
)

data class StockRequest(val quantity: Int)
