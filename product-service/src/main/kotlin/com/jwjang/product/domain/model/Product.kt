package com.jwjang.product.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 상품 카테고리 (Value Object로 단순 Enum 활용)
 */
enum class ProductCategory {
    ELECTRONICS, CLOTHING, FOOD, BOOKS, ETC
}

/**
 * 상품 Aggregate Root
 * 고객에게 '전시'되고 판매되는 대상
 */
@Entity
@Table(name = "products")
class Product(
    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false, precision = 15, scale = 2)
    var salesPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ProductCategory,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Column(nullable = false)
    var isOnSale: Boolean = true,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val skus: MutableList<Sku> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    /**
     * 상품 정보 수정
     */
    fun updateInfo(name: String, description: String, salesPrice: BigDecimal) {
        require(name.isNotBlank()) { "상품명은 공백일 수 없습니다." }
        require(salesPrice > BigDecimal.ZERO) { "판매가격은 0보다 커야 합니다." }
        this.name = name
        this.description = description
        this.salesPrice = salesPrice
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * SKU 추가
     */
    fun addSku(sku: Sku) {
        skus.add(sku)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 판매 중지
     */
    fun discontinue() {
        this.isOnSale = false
        this.updatedAt = LocalDateTime.now()
    }
}

/**
 * SKU (Stock Keeping Unit) - 재고 관리를 위한 최소 단위
 * 상품과 1:N 관계 (예: '나이키 티셔츠' 상품의 'S/Blue' SKU)
 */
@Entity
@Table(name = "skus")
class Sku(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false, unique = true, length = 100)
    val skuCode: String,

    @Column(nullable = false, length = 255)
    val optionName: String,  // 예: "S/Blue", "M/Red"

    @Column(nullable = false, precision = 15, scale = 2)
    var salesPrice: BigDecimal,

    @Column(nullable = false)
    var stockQuantity: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    /**
     * 재고 입고
     */
    fun addStock(quantity: Int) {
        require(quantity > 0) { "입고 수량은 0보다 커야 합니다." }
        this.stockQuantity += quantity
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 재고 차감 (주문 발생 시)
     */
    fun deductStock(quantity: Int) {
        require(quantity > 0) { "차감 수량은 0보다 커야 합니다." }
        check(this.stockQuantity >= quantity) {
            "재고가 부족합니다. 요청=${quantity}, 현재=${this.stockQuantity}"
        }
        this.stockQuantity -= quantity
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 재고 복구 (주문 취소 시)
     */
    fun restoreStock(quantity: Int) {
        require(quantity > 0) { "복구 수량은 0보다 커야 합니다." }
        this.stockQuantity += quantity
        this.updatedAt = LocalDateTime.now()
    }
}
