package com.jwjang.shipping.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 배송 상태
 */
enum class ShipmentStatus {
    /** 배송 준비 중 */
    PREPARING,
    /** 배송 중 */
    IN_TRANSIT,
    /** 배송 완료 */
    DELIVERED,
    /** 배송 취소 */
    CANCELLED
}

/**
 * 배송 Aggregate Root
 * 결제가 완료된 주문을 고객에게 전달하는 물리적 프로세스
 */
@Entity
@Table(name = "shipments")
class Shipment(
    @Column(nullable = false)
    val orderId: Long,

    @Column(nullable = false, length = 50)
    val orderNumber: String,

    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false, length = 500)
    val shippingAddress: String,

    /** 송장 번호 (Tracking Number) */
    @Column(unique = true, length = 100)
    var trackingNumber: String? = "TRK-${UUID.randomUUID().toString().uppercase().substring(0, 10)}",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ShipmentStatus = ShipmentStatus.PREPARING,

    @OneToMany(mappedBy = "shipment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val shipmentItems: MutableList<ShipmentItem> = mutableListOf(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    /**
     * 배송 시작
     */
    fun startShipping() {
        check(status == ShipmentStatus.PREPARING) { "준비 중 상태일 때만 배송을 시작할 수 있습니다." }
        this.status = ShipmentStatus.IN_TRANSIT
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 배송 완료
     */
    fun complete() {
        check(status == ShipmentStatus.IN_TRANSIT) { "배송 중 상태일 때만 완료 처리할 수 있습니다." }
        this.status = ShipmentStatus.DELIVERED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 배송 취소
     */
    fun cancel() {
        check(status == ShipmentStatus.PREPARING) { "준비 중 상태일 때만 취소할 수 있습니다." }
        this.status = ShipmentStatus.CANCELLED
        this.updatedAt = LocalDateTime.now()
    }

    fun addItem(item: ShipmentItem) {
        shipmentItems.add(item)
    }
}

/**
 * 배송 항목 Entity
 */
@Entity
@Table(name = "shipment_items")
class ShipmentItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    val shipment: Shipment,

    @Column(nullable = false)
    val skuId: Long,

    @Column(nullable = false, length = 100)
    val skuCode: String,

    @Column(nullable = false, length = 255)
    val productName: String,

    @Column(nullable = false)
    val quantity: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
