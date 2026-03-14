package com.jwjang.payment

import com.jwjang.payment.application.service.PaymentApplicationService
import com.jwjang.payment.domain.model.PaymentStatus
import com.jwjang.payment.domain.repository.PaymentRepository
import com.jwjang.payment.infrastructure.event.PaymentRequestedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import java.math.BigDecimal

/**
 * payment-service 통합 테스트
 * SAGA: 결제 요청 처리 → DB 상태 검증 → 보상 트랜잭션(취소) 검증
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = ["order.payment.requested", "order.payment.completed", "order.cancelled"]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PaymentServiceIntegrationTest {

    @Autowired
    lateinit var paymentApplicationService: PaymentApplicationService

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Test
    @Order(1)
    fun `결제 요청 처리 - 정상 금액(35000원) 승인 및 PG 거래 ID 발급`() {
        val event = PaymentRequestedEvent(
            orderId = 1L,
            orderNumber = "ORD-TEST-001",
            memberId = 100L,
            amount = BigDecimal("35000")
        )
        paymentApplicationService.handlePaymentRequested(event)

        val payment = paymentRepository.findByOrderId(1L).orElseThrow {
            AssertionError("결제 정보가 저장되지 않았습니다.")
        }

        assertThat(payment.status).isEqualTo(PaymentStatus.APPROVED)
        assertThat(payment.pgTransactionId).isNotNull().startsWith("PG-")
        assertThat(payment.failureReason).isNull()
        assertThat(payment.orderNumber).isEqualTo("ORD-TEST-001")
        assertThat(payment.amount).isEqualByComparingTo(BigDecimal("35000"))
    }

    @Test
    @Order(2)
    fun `결제 요청 처리 - 100만원 초과 금액은 실패 처리`() {
        val event = PaymentRequestedEvent(
            orderId = 2L,
            orderNumber = "ORD-TEST-002",
            memberId = 101L,
            amount = BigDecimal("1500000")  // 150만원 - 한도 초과
        )
        paymentApplicationService.handlePaymentRequested(event)

        val payment = paymentRepository.findByOrderId(2L).orElseThrow()
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.failureReason).isEqualTo("결제 한도 초과")
        assertThat(payment.pgTransactionId).isNull()
    }

    @Test
    @Order(3)
    fun `결제 취소 - 주문 취소 이벤트 수신 시 APPROVED → CANCELLED`() {
        // 먼저 결제 생성 (APPROVED)
        val approvedEvent = PaymentRequestedEvent(
            orderId = 3L,
            orderNumber = "ORD-TEST-003",
            memberId = 102L,
            amount = BigDecimal("50000")
        )
        paymentApplicationService.handlePaymentRequested(approvedEvent)
        val beforeCancel = paymentRepository.findByOrderId(3L).orElseThrow()
        assertThat(beforeCancel.status).isEqualTo(PaymentStatus.APPROVED)

        // 주문 취소 이벤트로 결제 취소 처리
        paymentApplicationService.handleOrderCancelled(mapOf("orderId" to 3L, "orderNumber" to "ORD-TEST-003"))

        val afterCancel = paymentRepository.findByOrderId(3L).orElseThrow()
        assertThat(afterCancel.status).isEqualTo(PaymentStatus.CANCELLED)
    }

    @Test
    @Order(4)
    fun `결제 실패 상태에서 취소 시도 - 무시 처리 (FAILED 상태 유지)`() {
        // FAILED 상태 결제가 있는 경우 취소 이벤트 수신
        val failedEvent = PaymentRequestedEvent(
            orderId = 4L,
            orderNumber = "ORD-TEST-004",
            memberId = 103L,
            amount = BigDecimal("2000000")  // 실패
        )
        paymentApplicationService.handlePaymentRequested(failedEvent)

        // 취소 이벤트 → FAILED 상태는 cancel() 불가, 예외 없이 무시
        paymentApplicationService.handleOrderCancelled(mapOf("orderId" to 4L, "orderNumber" to "ORD-TEST-004"))

        val payment = paymentRepository.findByOrderId(4L).orElseThrow()
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)  // 변경 안 됨
    }
}
