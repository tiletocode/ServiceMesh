package com.jwjang.shipping.interfaces.rest

import com.jwjang.shipping.application.service.ShipmentView
import com.jwjang.shipping.application.service.ShippingApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shipments")
class ShipmentController(
    private val shippingApplicationService: ShippingApplicationService
) {
    /** 송장 번호로 배송 조회 */
    @GetMapping("/tracking/{trackingNumber}")
    fun getByTracking(@PathVariable trackingNumber: String): ResponseEntity<ShipmentView> {
        return ResponseEntity.ok(shippingApplicationService.getShipmentByTracking(trackingNumber))
    }
}
