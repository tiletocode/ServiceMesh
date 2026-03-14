package com.jwjang.shipping.domain.repository

import com.jwjang.shipping.domain.model.Shipment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface ShipmentRepository {
    fun save(shipment: Shipment): Shipment
    fun findById(id: Long): Optional<Shipment>
    fun findByOrderId(orderId: Long): Optional<Shipment>
    fun findByTrackingNumber(trackingNumber: String): Optional<Shipment>
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<Shipment>
}
