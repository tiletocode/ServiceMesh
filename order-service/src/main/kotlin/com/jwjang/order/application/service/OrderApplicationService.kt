package com.jwjang.order.application.service

import com.jwjang.order.domain.exception.OrderNotFoundException
import com.jwjang.order.domain.model.Order
import com.jwjang.order.domain.model.OrderLine
import com.jwjang.order.domain.model.OrderStatus
import com.jwjang.order.domain.repository.OrderRepository
import com.jwjang.order.infrastructure.client.ProductServiceClient
import com.jwjang.order.infrastructure.client.StockRequest
import com.jwjang.order.infrastructure.event.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

// --- Commands ---
data class CreateOrderCommand(
    val memberId: Long,
    val shippingAddress: String,
    val orderLineRequests: List<OrderLineRequest>
)

data class OrderLineRequest(
    val skuId: Long,
    val quantity: Int
)

// --- Views ---
data class OrderView(
    val id: Long,
    val orderNumber: String,
    val memberId: Long,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val shippingAddress: String,
    val orderLines: List<OrderLineView>,
    val createdAt: LocalDateTime
)

data class OrderLineView(
    val id: Long,
    val skuId: Long,
    val skuCode: String,
    val productName: String,
    val quantity: Int,
    val orderPrice: BigDecimal
)

fun Order.toView() = OrderView(
    id = id!!,
    orderNumber = orderNumber,
    memberId = memberId,
    status = status,
    totalAmount = totalAmount,
    shippingAddress = shippingAddress,
    orderLines = orderLines.map {
        OrderLineView(
            id = it.id!!,
            skuId = it.skuId,
            skuCode = it.skuCode,
            productName = it.productName,
            quantity = it.quantity,
            orderPrice = it.orderPrice
        )
    },
    createdAt = createdAt
)

/**
 * 주문 애플리케이션 서비스
 * SAGA Choreography 패턴의 오케스트레이터 역할
 */
@Service
@Transactional
class OrderApplicationService(
    private val orderRepository: OrderRepository,
    private val productServiceClient: ProductServiceClient,
    private val orderEventPublisher: OrderEventPublisher
) {

    /**
     * 주문 생성 (SAGA 시작점)
     * 1. 재고 확인 및 차감 (Feign: product-service)
     * 2. 주문 저장
     * 3. 결제 요청 이벤트 발행 (Kafka -> payment-service)
     */
    fun createOrder(command: CreateOrderCommand): OrderView {
        val order = Order(
            memberId = command.memberId,
            shippingAddress = command.shippingAddress
        )

        val savedOrder = orderRepository.save(order)

        // 각 주문 항목에 대해 재고 차감 및 OrderLine 생성
        val orderLineEvents = mutableListOf<OrderLineEvent>()
        val deductedSkus = mutableListOf<Pair<Long, Int>>()

        try {
            for (lineRequest in command.orderLineRequests) {
                val skuInfo = productServiceClient.getSku(lineRequest.skuId)
                productServiceClient.deductStock(lineRequest.skuId, StockRequest(lineRequest.quantity))
                deductedSkus.add(Pair(lineRequest.skuId, lineRequest.quantity))

                val orderLine = OrderLine(
                    order = savedOrder,
                    skuId = lineRequest.skuId,
                    skuCode = skuInfo.skuCode,
                    productName = skuInfo.productName ?: skuInfo.optionName,
                    quantity = lineRequest.quantity,
                    orderPrice = skuInfo.salesPrice ?: BigDecimal.ZERO
                )
                savedOrder.addOrderLine(orderLine)

                orderLineEvents.add(OrderLineEvent(
                    skuId = lineRequest.skuId,
                    skuCode = skuInfo.skuCode,
                    quantity = lineRequest.quantity,
                    orderPrice = orderLine.orderPrice
                ))
            }
        } catch (e: Exception) {
            // 보상 트랜잭션: 차감된 재고 복구
            deductedSkus.forEach { (skuId, qty) ->
                runCatching {
                    productServiceClient.restoreStock(skuId, StockRequest(qty))
                }
            }
            throw e
        }

        orderRepository.save(savedOrder)

        // SAGA: 결제 요청 이벤트 발행
        orderEventPublisher.publishPaymentRequested(
            PaymentRequestedEvent(
                orderId = savedOrder.id!!,
                orderNumber = savedOrder.orderNumber,
                memberId = savedOrder.memberId,
                amount = savedOrder.totalAmount
            )
        )

        return savedOrder.toView()
    }

    /**
     * 주문 조회
     */
    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderView {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException(orderId) }
        return order.toView()
    }

    /**
     * 회원 주문 목록 조회
     */
    @Transactional(readOnly = true)
    fun getOrdersByMember(memberId: Long, pageable: Pageable): Page<OrderView> {
        return orderRepository.findByMemberId(memberId, pageable).map { it.toView() }
    }

    /**
     * 주문 취소 (SAGA 보상 트랜잭션)
     */
    fun cancelOrder(orderId: Long): OrderView {
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException(orderId) }

        order.cancel()
        orderRepository.save(order)

        // 재고 복구
        order.orderLines.forEach { line ->
            runCatching {
                productServiceClient.restoreStock(line.skuId, StockRequest(line.quantity))
            }
        }

        orderEventPublisher.publishOrderCancelled(orderId, order.orderNumber)
        return order.toView()
    }
}
