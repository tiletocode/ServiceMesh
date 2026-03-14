package com.jwjang.payment.application.service

import com.jwjang.payment.domain.model.Payment
import com.jwjang.payment.domain.repository.PaymentRepository
import com.jwjang.payment.infrastructure.event.PaymentCompletedEvent
import com.jwjang.payment.infrastructure.event.PaymentEventTopics
import com.jwjang.payment.infrastructure.event.PaymentRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 결제 애플리케이션 서비스
 * SAGA: order-service의 결제 요청을 수신하고, 결과를 발행
 */
@Service
@Transactional
class PaymentApplicationService(
    private val paymentRepository: PaymentRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 결제 요청 이벤트 처리 (Kafka Consumer)
     * 실제 환경에서는 외부 PG사 API를 호출
     */
    @KafkaListener(
        topics = [PaymentEventTopics.ORDER_PAYMENT_REQUESTED],
        groupId = "\${spring.kafka.consumer.group-id:payment-service-group}",
        containerFactory = "paymentRequestedFactory"
    )
    fun handlePaymentRequested(event: PaymentRequestedEvent) {
        log.info("Processing payment request: orderId=${event.orderId}, amount=${event.amount}")

        val payment = Payment(
            orderId = event.orderId,
            orderNumber = event.orderNumber,
            memberId = event.memberId,
            amount = event.amount
        )
        paymentRepository.save(payment)

        // 실제 환경에서는 외부 PG사 호출
        // 여기서는 시뮬레이션: 금액이 100만원 초과이면 실패 처리
        val success = event.amount.toLong() <= 1_000_000L
        val pgTransactionId = "PG-${UUID.randomUUID().toString().uppercase().substring(0, 12)}"

        if (success) {
            payment.approve(pgTransactionId)
        } else {
            payment.fail("결제 한도 초과")
        }

        paymentRepository.save(payment)

        // 결제 결과를 order-service에 발행
        val resultEvent = PaymentCompletedEvent(
            orderId = event.orderId,
            orderNumber = event.orderNumber,
            paymentId = payment.id!!,
            success = success,
            failureReason = payment.failureReason
        )

        kafkaTemplate.send(PaymentEventTopics.ORDER_PAYMENT_COMPLETED, event.orderNumber, resultEvent)
        log.info("Payment ${if (success) "approved" else "failed"}: orderId=${event.orderId}")
    }

    /**
     * 주문 취소 이벤트 처리 (SAGA 보상 트랜잭션 - 결제 취소)
     */
    @KafkaListener(
        topics = [PaymentEventTopics.ORDER_CANCELLED],
        groupId = "\${spring.kafka.consumer.group-id:payment-service-group}",
        containerFactory = "mapEventFactory"
    )
    fun handleOrderCancelled(event: Map<String, Any>) {
        val orderId = (event["orderId"] as? Number)?.toLong() ?: return
        log.info("Processing payment cancellation for orderId=$orderId")

        paymentRepository.findByOrderId(orderId).ifPresent { payment ->
            runCatching { payment.cancel() }
                .onSuccess {
                    paymentRepository.save(payment)
                    log.info("Payment cancelled for orderId=$orderId")
                }
                .onFailure {
                    log.warn("Cannot cancel payment for orderId=$orderId: ${it.message}")
                }
        }
    }
}
