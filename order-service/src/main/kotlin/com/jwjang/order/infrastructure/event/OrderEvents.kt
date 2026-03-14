package com.jwjang.order.infrastructure.event

/**
 * Kafka 이벤트 토픽 정의
 */
object OrderEventTopics {
    const val ORDER_CREATED = "order.created"
    const val ORDER_PAYMENT_REQUESTED = "order.payment.requested"
    const val ORDER_PAYMENT_COMPLETED = "order.payment.completed"
    const val ORDER_PAYMENT_FAILED = "order.payment.failed"
    const val ORDER_CANCELLED = "order.cancelled"
    const val SHIPMENT_STARTED = "shipment.started"
}

/**
 * 주문 생성 이벤트 (Kafka 발행)
 */
data class OrderCreatedEvent(
    val orderId: Long,
    val orderNumber: String,
    val memberId: Long,
    val totalAmount: java.math.BigDecimal,
    val shippingAddress: String,
    val orderLines: List<OrderLineEvent>
)

data class OrderLineEvent(
    val skuId: Long,
    val skuCode: String,
    val quantity: Int,
    val orderPrice: java.math.BigDecimal
)

/**
 * 결제 요청 이벤트 (SAGA: order -> payment)
 */
data class PaymentRequestedEvent(
    val orderId: Long,
    val orderNumber: String,
    val memberId: Long,
    val amount: java.math.BigDecimal
)

/**
 * 결제 완료 이벤트 (SAGA: payment -> order)
 */
data class PaymentCompletedEvent(
    val orderId: Long,
    val orderNumber: String,
    val paymentId: Long,
    val success: Boolean,
    val failureReason: String? = null
)
