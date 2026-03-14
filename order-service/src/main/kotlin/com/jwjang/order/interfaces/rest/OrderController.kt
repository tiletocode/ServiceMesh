package com.jwjang.order.interfaces.rest

import com.jwjang.order.application.service.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderApplicationService: OrderApplicationService
) {
    /** 주문 생성 */
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderView> {
        val command = CreateOrderCommand(
            memberId = request.memberId,
            shippingAddress = request.shippingAddress,
            orderLineRequests = request.orderLines.map {
                OrderLineRequest(skuId = it.skuId, quantity = it.quantity)
            }
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderApplicationService.createOrder(command))
    }

    /** 주문 상세 조회 */
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: Long): ResponseEntity<OrderView> {
        return ResponseEntity.ok(orderApplicationService.getOrder(orderId))
    }

    /** 회원별 주문 목록 조회 */
    @GetMapping("/members/{memberId}")
    fun getOrdersByMember(
        @PathVariable memberId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<OrderView>> {
        return ResponseEntity.ok(orderApplicationService.getOrdersByMember(memberId, pageable))
    }

    /** 주문 취소 */
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(@PathVariable orderId: Long): ResponseEntity<OrderView> {
        return ResponseEntity.ok(orderApplicationService.cancelOrder(orderId))
    }
}

data class CreateOrderRequest(
    val memberId: Long,
    val shippingAddress: String,
    val orderLines: List<OrderLineRequestDto>
)

data class OrderLineRequestDto(
    val skuId: Long,
    val quantity: Int
)
