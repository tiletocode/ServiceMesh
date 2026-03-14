package com.jwjang.product.application.service

import com.jwjang.product.domain.exception.ProductNotFoundException
import com.jwjang.product.domain.exception.SkuNotFoundException
import com.jwjang.product.domain.model.Product
import com.jwjang.product.domain.model.ProductCategory
import com.jwjang.product.domain.model.Sku
import com.jwjang.product.domain.repository.ProductRepository
import com.jwjang.product.domain.repository.SkuRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

// --- Commands ---
data class CreateProductCommand(
    val name: String,
    val description: String,
    val salesPrice: BigDecimal,
    val category: ProductCategory,
    val imageUrl: String? = null
)

data class AddSkuCommand(
    val productId: Long,
    val skuCode: String,
    val optionName: String,
    val salesPrice: BigDecimal,
    val initialStock: Int = 0
)

data class DeductStockCommand(
    val skuId: Long,
    val quantity: Int
)

data class RestoreStockCommand(
    val skuId: Long,
    val quantity: Int
)

// --- Views ---
data class ProductView(
    @com.fasterxml.jackson.annotation.JsonProperty("id") val id: Long,
    @com.fasterxml.jackson.annotation.JsonProperty("name") val name: String,
    @com.fasterxml.jackson.annotation.JsonProperty("description") val description: String,
    @com.fasterxml.jackson.annotation.JsonProperty("salesPrice") val salesPrice: BigDecimal,
    @com.fasterxml.jackson.annotation.JsonProperty("category") val category: ProductCategory,
    @com.fasterxml.jackson.annotation.JsonProperty("imageUrl") val imageUrl: String?,
    @com.fasterxml.jackson.annotation.JsonProperty("isOnSale") val isOnSale: Boolean,
    @com.fasterxml.jackson.annotation.JsonProperty("skus") val skus: List<SkuView>
)

data class SkuView(
    @com.fasterxml.jackson.annotation.JsonProperty("id") val id: Long,
    @com.fasterxml.jackson.annotation.JsonProperty("skuCode") val skuCode: String,
    @com.fasterxml.jackson.annotation.JsonProperty("optionName") val optionName: String,
    @com.fasterxml.jackson.annotation.JsonProperty("salesPrice") val salesPrice: BigDecimal,
    @com.fasterxml.jackson.annotation.JsonProperty("stockQuantity") val stockQuantity: Int,
    @com.fasterxml.jackson.annotation.JsonProperty("productName") val productName: String? = null
)

fun Product.toView() = ProductView(
    id = id!!,
    name = name,
    description = description,
    salesPrice = salesPrice,
    category = category,
    imageUrl = imageUrl,
    isOnSale = isOnSale,
    skus = skus.map { it.toView() }
)

fun Sku.toView() = SkuView(
    id = id!!,
    skuCode = skuCode,
    optionName = optionName,
    salesPrice = salesPrice,
    stockQuantity = stockQuantity,
    productName = product.name
)

/**
 * 상품 애플리케이션 서비스 (CQRS 패턴 적용)
 * - Command: 상품 생성, SKU 추가, 재고 차감/복구
 * - Query: 상품 조회 (Redis 캐싱)
 */
@Service
@Transactional
class ProductApplicationService(
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository
) {

    // ======================== Commands ========================

    @CacheEvict(cacheNames = ["products"], allEntries = true)
    fun createProduct(command: CreateProductCommand): ProductView {
        val product = Product(
            name = command.name,
            description = command.description,
            salesPrice = command.salesPrice,
            category = command.category,
            imageUrl = command.imageUrl
        )
        return productRepository.save(product).toView()
    }

    @CacheEvict(cacheNames = ["products", "product"], key = "#command.productId")
    fun addSku(command: AddSkuCommand): SkuView {
        val product = productRepository.findById(command.productId)
            .orElseThrow { ProductNotFoundException(command.productId) }

        val sku = Sku(
            product = product,
            skuCode = command.skuCode,
            optionName = command.optionName,
            salesPrice = command.salesPrice,
            stockQuantity = command.initialStock
        )
        product.addSku(sku)
        return skuRepository.save(sku).toView()
    }

    @CacheEvict(cacheNames = ["sku"], key = "#command.skuId")
    fun deductStock(command: DeductStockCommand) {
        val sku = skuRepository.findById(command.skuId)
            .orElseThrow { SkuNotFoundException(command.skuId) }
        sku.deductStock(command.quantity)
        skuRepository.save(sku)
    }

    @CacheEvict(cacheNames = ["sku"], key = "#command.skuId")
    fun restoreStock(command: RestoreStockCommand) {
        val sku = skuRepository.findById(command.skuId)
            .orElseThrow { SkuNotFoundException(command.skuId) }
        sku.restoreStock(command.quantity)
        skuRepository.save(sku)
    }

    // ======================== Queries ========================

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["product"], key = "#productId")
    fun getProduct(productId: Long): ProductView {
        val product = productRepository.findById(productId)
            .orElseThrow { ProductNotFoundException(productId) }
        return product.toView()
    }

    @Transactional(readOnly = true)
    fun getOnSaleProducts(pageable: Pageable): Page<ProductView> {
        // Page<PageImpl> 은 Redis 역직렬화 불가 → 캐시 제외, DB 직접 조회
        return productRepository.findAllByIsOnSaleTrue(pageable).map { it.toView() }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["sku"], key = "#skuId")
    fun getSku(skuId: Long): SkuView {
        val sku = skuRepository.findById(skuId)
            .orElseThrow { SkuNotFoundException(skuId) }
        return sku.toView()
    }
}
