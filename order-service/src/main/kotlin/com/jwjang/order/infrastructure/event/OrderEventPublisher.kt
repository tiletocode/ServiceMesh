package com.jwjang.order.infrastructure.event

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * 주문 이벤트 Kafka 발행자
 */
@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishOrderCreated(event: OrderCreatedEvent) {
        log.info("Publishing OrderCreatedEvent: orderNumber=${event.orderNumber}")
        kafkaTemplate.send(OrderEventTopics.ORDER_CREATED, event.orderNumber, event)
    }

    fun publishPaymentRequested(event: PaymentRequestedEvent) {
        log.info("Publishing PaymentRequestedEvent: orderNumber=${event.orderNumber}")
        kafkaTemplate.send(OrderEventTopics.ORDER_PAYMENT_REQUESTED, event.orderNumber, event)
    }

    fun publishOrderCancelled(orderId: Long, orderNumber: String) {
        log.info("Publishing OrderCancelledEvent: orderNumber=$orderNumber")
        kafkaTemplate.send(OrderEventTopics.ORDER_CANCELLED, orderNumber, mapOf(
            "orderId" to orderId,
            "orderNumber" to orderNumber
        ))
    }
}
