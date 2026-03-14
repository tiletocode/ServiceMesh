package com.jwjang.shipping.application.service

import com.jwjang.shipping.domain.model.Shipment
import com.jwjang.shipping.domain.model.ShipmentItem
import com.jwjang.shipping.domain.model.ShipmentStatus
import com.jwjang.shipping.domain.repository.ShipmentRepository
import com.jwjang.shipping.infrastructure.event.PaymentCompletedEvent
import com.jwjang.shipping.infrastructure.event.ShippingEventTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class ShipmentView(
    val id: Long,
    val orderId: Long,
    val orderNumber: String,
    val trackingNumber: String?,
    val status: ShipmentStatus,
    val shippingAddress: String,
    val createdAt: LocalDateTime
)

fun Shipment.toView() = ShipmentView(
    id = id!!,
    orderId = orderId,
    orderNumber = orderNumber,
    trackingNumber = trackingNumber,
    status = status,
    shippingAddress = shippingAddress,
    createdAt = createdAt
)

/**
 * 배송 애플리케이션 서비스
 * EDA: 결제 완료 이벤트를 구독하여 배송 프로세스 시작
 */
@Service
@Transactional
class ShippingApplicationService(
    private val shipmentRepository: ShipmentRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 결제 완료 이벤트 수신 → 배송 생성 (EDA)
     * order-service와 완전히 분리된 비동기 처리
     */
    @KafkaListener(
        topics = [ShippingEventTopics.ORDER_PAYMENT_COMPLETED],
        groupId = "\${spring.kafka.consumer.group-id:shipping-service-group}",
        containerFactory = "paymentCompletedFactory"
    )
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        if (!event.success) {
            log.info("Payment failed for orderId=${event.orderId}, skipping shipment creation")
            return
        }

        log.info("Creating shipment for orderId=${event.orderId}")

        // 실제 환경에서는 order-service에서 주문 상세 조회 후 shippingAddress 획득
        // 여기서는 이벤트 기반이므로 별도 API 호출 최소화 방향으로 구현
        val shipment = Shipment(
            orderId = event.orderId,
            orderNumber = event.orderNumber,
            memberId = 0L,  // 실제 구현 시 OrderCreatedEvent에서 memberId 획득
            shippingAddress = "이벤트에서 주소 획득 필요"
        )

        shipmentRepository.save(shipment)

        // 배송 시작 이벤트 발행 (order-service 상태 업데이트용)
        kafkaTemplate.send(ShippingEventTopics.SHIPMENT_STARTED, event.orderNumber, mapOf(
            "orderId" to event.orderId,
            "orderNumber" to event.orderNumber,
            "shipmentId" to shipment.id,
            "trackingNumber" to shipment.trackingNumber
        ))

        log.info("Shipment created: trackingNumber=${shipment.trackingNumber}, orderId=${event.orderId}")
    }

    /**
     * 주문 취소 이벤트 수신 → 배송 취소
     */
    @KafkaListener(
        topics = [ShippingEventTopics.ORDER_CANCELLED],
        groupId = "\${spring.kafka.consumer.group-id:shipping-service-group}",
        containerFactory = "mapEventFactory"
    )
    fun handleOrderCancelled(event: Map<String, Any>) {
        val orderId = (event["orderId"] as? Number)?.toLong() ?: return
        log.info("Processing shipment cancellation for orderId=$orderId")

        shipmentRepository.findByOrderId(orderId).ifPresent { shipment ->
            runCatching { shipment.cancel() }
                .onSuccess {
                    shipmentRepository.save(shipment)
                    log.info("Shipment cancelled for orderId=$orderId")
                }
                .onFailure {
                    log.warn("Cannot cancel shipment for orderId=$orderId: ${it.message}")
                }
        }
    }

    @Transactional(readOnly = true)
    fun getShipmentByTracking(trackingNumber: String): ShipmentView {
        val shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow { RuntimeException("배송 정보를 찾을 수 없습니다. trackingNumber=$trackingNumber") }
        return shipment.toView()
    }

    @Transactional(readOnly = true)
    fun getShipmentByOrderNumber(orderNumber: String): ShipmentView {
        val shipment = shipmentRepository.findByOrderNumber(orderNumber)
            .orElseThrow { RuntimeException("배송 정보를 찾을 수 없습니다. orderNumber=$orderNumber") }
        return shipment.toView()
    }

    @Transactional(readOnly = true)
    fun getShipmentsByMember(memberId: Long, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<ShipmentView> {
        return shipmentRepository.findByMemberId(memberId, pageable).map { it.toView() }
    }
}
