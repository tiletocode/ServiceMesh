package com.jwjang.shipping.infrastructure.event

import java.math.BigDecimal

object ShippingEventTopics {
    const val ORDER_PAYMENT_COMPLETED = "order.payment.completed"
    const val ORDER_CANCELLED = "order.cancelled"
    const val SHIPMENT_STARTED = "shipment.started"
}

/** order.payment.completed에서 받는 이벤트 */
data class PaymentCompletedEvent(
    val orderId: Long,
    val orderNumber: String,
    val paymentId: Long,
    val success: Boolean,
    val failureReason: String? = null
)

/** 이벤트에 포함된 주문 항목 정보 */
data class OrderLineEvent(
    val skuId: Long,
    val skuCode: String,
    val quantity: Int,
    val orderPrice: BigDecimal
)
