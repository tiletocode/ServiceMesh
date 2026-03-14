package com.jwjang.payment.infrastructure.event

import java.math.BigDecimal

object PaymentEventTopics {
    const val ORDER_PAYMENT_REQUESTED = "order.payment.requested"
    const val ORDER_PAYMENT_COMPLETED = "order.payment.completed"
    const val ORDER_CANCELLED = "order.cancelled"
}

/** order-service에서 받는 결제 요청 이벤트 */
data class PaymentRequestedEvent(
    val orderId: Long,
    val orderNumber: String,
    val memberId: Long,
    val amount: BigDecimal
)

/** order-service로 보내는 결제 완료 이벤트 */
data class PaymentCompletedEvent(
    val orderId: Long,
    val orderNumber: String,
    val paymentId: Long,
    val success: Boolean,
    val failureReason: String? = null
)
