package com.jwjang.shipping

import com.jwjang.shipping.application.service.ShippingApplicationService
import com.jwjang.shipping.domain.model.ShipmentStatus
import com.jwjang.shipping.domain.repository.ShipmentRepository
import com.jwjang.shipping.infrastructure.event.PaymentCompletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

/**
 * shipping-service 통합 테스트
 * EDA: 결제 완료 이벤트 → 배송 생성 / 주문 취소 이벤트 → 배송 취소 검증
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = ["order.payment.completed", "order.cancelled", "shipment.started"]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ShippingServiceIntegrationTest {

    @Autowired
    lateinit var shippingApplicationService: ShippingApplicationService

    @Autowired
    lateinit var shipmentRepository: ShipmentRepository

    @Test
    @Order(1)
    fun `결제 완료 이벤트 수신 - 배송 자동 생성 및 PREPARING 상태`() {
        val event = PaymentCompletedEvent(
            orderId = 100L,
            orderNumber = "ORD-SHIP-001",
            paymentId = 1L,
            success = true
        )
        shippingApplicationService.handlePaymentCompleted(event)

        val shipment = shipmentRepository.findByOrderId(100L).orElseThrow {
            AssertionError("배송 정보가 생성되지 않았습니다.")
        }

        assertThat(shipment.status).isEqualTo(ShipmentStatus.PREPARING)
        assertThat(shipment.trackingNumber).isNotNull().startsWith("TRK-")
        assertThat(shipment.orderNumber).isEqualTo("ORD-SHIP-001")
    }

    @Test
    @Order(2)
    fun `결제 실패 이벤트 수신 - 배송 생성 안 함`() {
        val event = PaymentCompletedEvent(
            orderId = 200L,
            orderNumber = "ORD-SHIP-002",
            paymentId = 2L,
            success = false,
            failureReason = "카드 한도 초과"
        )
        shippingApplicationService.handlePaymentCompleted(event)

        val shipment = shipmentRepository.findByOrderId(200L)
        assertThat(shipment).isEmpty()
    }

    @Test
    @Order(3)
    fun `주문 취소 이벤트 수신 - PREPARING 상태의 배송 취소`() {
        // 먼저 배송 생성
        val paymentEvent = PaymentCompletedEvent(
            orderId = 300L,
            orderNumber = "ORD-SHIP-003",
            paymentId = 3L,
            success = true
        )
        shippingApplicationService.handlePaymentCompleted(paymentEvent)
        val beforeCancel = shipmentRepository.findByOrderId(300L).orElseThrow()
        assertThat(beforeCancel.status).isEqualTo(ShipmentStatus.PREPARING)

        // 주문 취소 이벤트
        shippingApplicationService.handleOrderCancelled(
            mapOf("orderId" to 300L, "orderNumber" to "ORD-SHIP-003")
        )

        val afterCancel = shipmentRepository.findByOrderId(300L).orElseThrow()
        assertThat(afterCancel.status).isEqualTo(ShipmentStatus.CANCELLED)
    }

    @Test
    @Order(4)
    fun `송장 번호로 배송 조회 성공`() {
        val event = PaymentCompletedEvent(
            orderId = 400L,
            orderNumber = "ORD-SHIP-004",
            paymentId = 4L,
            success = true
        )
        shippingApplicationService.handlePaymentCompleted(event)

        val shipment = shipmentRepository.findByOrderId(400L).orElseThrow()
        val trackingNumber = shipment.trackingNumber!!

        val view = shippingApplicationService.getShipmentByTracking(trackingNumber)
        assertThat(view.trackingNumber).isEqualTo(trackingNumber)
        assertThat(view.orderId).isEqualTo(400L)
        assertThat(view.status).isEqualTo(ShipmentStatus.PREPARING)
    }

    @Test
    @Order(5)
    fun `존재하지 않는 송장 번호 조회 시 예외 발생`() {
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            shippingApplicationService.getShipmentByTracking("NOT-EXISTING-TRACKING")
        }
    }
}
