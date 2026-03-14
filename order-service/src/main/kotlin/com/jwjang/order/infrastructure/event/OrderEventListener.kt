package com.jwjang.order.infrastructure.event

import com.jwjang.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Kafka 이벤트 리스너 - SAGA 패턴에서 결제 결과 수신
 */
@Component
class OrderEventListener(
    private val orderRepository: OrderRepository,
    private val orderEventPublisher: OrderEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 결제 완료/실패 이벤트 수신 (SAGA Choreography)
     */
    @KafkaListener(
        topics = [OrderEventTopics.ORDER_PAYMENT_COMPLETED],
        groupId = "\${spring.kafka.consumer.group-id:order-service-group}",
        containerFactory = "paymentCompletedFactory"
    )
    @Transactional
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        log.info("Received PaymentCompletedEvent: orderId=${event.orderId}, success=${event.success}")

        val order = orderRepository.findById(event.orderId).orElse(null) ?: run {
            log.warn("Order not found: orderId=${event.orderId}")
            return
        }

        if (event.success) {
            order.markAsPaid()
            orderRepository.save(order)
            log.info("Order marked as PAID: orderNumber=${order.orderNumber}")
        } else {
            order.cancel(event.failureReason ?: "결제 실패")
            orderRepository.save(order)
            orderEventPublisher.publishOrderCancelled(order.id!!, order.orderNumber)
            log.warn("Order cancelled due to payment failure: orderNumber=${order.orderNumber}, reason=${event.failureReason}")
        }
    }
}
