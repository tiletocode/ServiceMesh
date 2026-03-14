package com.jwjang.payment.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 결제 상태
 */
enum class PaymentStatus {
    REQUESTED,
    APPROVED,
    FAILED,
    CANCELLED
}

/**
 * 결제 Aggregate Root
 * 주문에 대한 금액 지불 시도 및 그 결과
 */
@Entity
@Table(name = "payments")
class Payment(
    @Column(nullable = false)
    val orderId: Long,

    @Column(nullable = false, length = 50)
    val orderNumber: String,

    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.REQUESTED,

    @Column(length = 500)
    var failureReason: String? = null,

    /** 외부 PG사 거래 ID */
    @Column(length = 200)
    var pgTransactionId: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    /**
     * 결제 승인
     */
    fun approve(pgTransactionId: String) {
        check(status == PaymentStatus.REQUESTED) { "요청 상태에서만 승인할 수 있습니다." }
        this.status = PaymentStatus.APPROVED
        this.pgTransactionId = pgTransactionId
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제 실패
     */
    fun fail(reason: String) {
        check(status == PaymentStatus.REQUESTED) { "요청 상태에서만 실패 처리할 수 있습니다." }
        this.status = PaymentStatus.FAILED
        this.failureReason = reason
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 결제 취소 (보상 트랜잭션)
     */
    fun cancel() {
        check(status == PaymentStatus.APPROVED) { "승인 상태에서만 취소할 수 있습니다." }
        this.status = PaymentStatus.CANCELLED
        this.updatedAt = LocalDateTime.now()
    }
}
