package com.jwjang.shipping.interfaces.rest

import com.jwjang.shipping.application.service.ShipmentView
import com.jwjang.shipping.application.service.ShippingApplicationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shipments")
class ShipmentController(
    private val shippingApplicationService: ShippingApplicationService
) {
    /** 송장 번호로 배송 조회 */
    @GetMapping("/tracking/{trackingNumber}")
    fun getByTracking(@PathVariable trackingNumber: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(shippingApplicationService.getShipmentByTracking(trackingNumber))
        } catch (e: RuntimeException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }
    }

    /** 주문번호로 배송 조회 */
    @GetMapping("/orders/{orderNumber}")
    fun getByOrderNumber(@PathVariable orderNumber: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(shippingApplicationService.getShipmentByOrderNumber(orderNumber))
        } catch (e: RuntimeException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }
    }

    /** 회원별 배송 목록 조회 */
    @GetMapping("/members/{memberId}")
    fun getByMember(
        @PathVariable memberId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<ShipmentView>> {
        return ResponseEntity.ok(shippingApplicationService.getShipmentsByMember(memberId, pageable))
    }
}
