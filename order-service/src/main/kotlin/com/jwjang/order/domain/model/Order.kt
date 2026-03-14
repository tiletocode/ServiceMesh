package com.jwjang.order.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 주문 Aggregate Root
 * 회원이 하나 이상의 주문 항목을 구매하겠다는 단일 트랜잭션
 */
@Entity
@Table(name = "orders")
class Order(
    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false, unique = true, length = 50)
    val orderNumber: String = "ORD-${UUID.randomUUID().toString().uppercase().substring(0, 12)}",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = true, length = 500)
    var cancelReason: String? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 500)
    var shippingAddress: String,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderLines: MutableList<OrderLine> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    /**
     * 주문 항목 추가
     */
    fun addOrderLine(orderLine: OrderLine) {
        check(status == OrderStatus.PENDING) { "PENDING 상태일 때만 주문 항목을 추가할 수 있습니다." }
        orderLines.add(orderLine)
        recalculateTotalAmount()
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제 완료 처리 (SAGA: 결제 서비스 성공 응답)
     */
    fun markAsPaid() {
        check(status == OrderStatus.PENDING) { "PENDING 상태일 때만 결제 완료로 변경할 수 있습니다." }
        this.status = OrderStatus.PAID
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 배송 시작 처리 (EDA: 배송 서비스 이벤트 수신)
     */
    fun markAsShipped() {
        check(status == OrderStatus.PAID) { "PAID 상태일 때만 배송 시작으로 변경할 수 있습니다." }
        this.status = OrderStatus.SHIPPED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 주문 취소 (SAGA 보상 트랜잭션)
     */
    fun cancel(reason: String? = null) {
        check(status == OrderStatus.PENDING || status == OrderStatus.PAID) {
            "취소 불가능한 상태입니다. 현재 상태: $status"
        }
        this.status = OrderStatus.CANCELLED
        this.cancelReason = reason
        this.updatedAt = LocalDateTime.now()
    }

    private fun recalculateTotalAmount() {
        this.totalAmount = orderLines.sumOf { it.orderPrice * it.quantity.toBigDecimal() }
    }
}

/**
 * 주문 항목 (Order Line Entity)
 * 주문에 포함된 개별 SKU와 수량, 주문 당시의 '주문 가격'을 스냅샷으로 기록
 */
@Entity
@Table(name = "order_lines")
class OrderLine(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(nullable = false)
    val skuId: Long,

    @Column(nullable = false, length = 100)
    val skuCode: String,

    @Column(nullable = false, length = 255)
    val productName: String,

    @Column(nullable = false)
    val quantity: Int,

    /** 주문 가격: 주문 시점의 판매가 스냅샷 (불변) */
    @Column(nullable = false, precision = 15, scale = 2)
    val orderPrice: BigDecimal,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
