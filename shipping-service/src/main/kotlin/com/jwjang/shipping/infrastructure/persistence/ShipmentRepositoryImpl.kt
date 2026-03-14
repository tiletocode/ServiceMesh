package com.jwjang.shipping.infrastructure.persistence

import com.jwjang.shipping.domain.model.Shipment
import com.jwjang.shipping.domain.repository.ShipmentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

interface ShipmentJpaRepository : JpaRepository<Shipment, Long> {
    fun findByOrderId(orderId: Long): Optional<Shipment>
    fun findByOrderNumber(orderNumber: String): Optional<Shipment>
    fun findByTrackingNumber(trackingNumber: String): Optional<Shipment>
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<Shipment>
}

@Repository
class ShipmentRepositoryImpl(private val jpa: ShipmentJpaRepository) : ShipmentRepository {
    override fun save(shipment: Shipment): Shipment = jpa.save(shipment)
    override fun findById(id: Long): Optional<Shipment> = jpa.findById(id)
    override fun findByOrderId(orderId: Long): Optional<Shipment> = jpa.findByOrderId(orderId)
    override fun findByOrderNumber(orderNumber: String): Optional<Shipment> = jpa.findByOrderNumber(orderNumber)
    override fun findByTrackingNumber(trackingNumber: String): Optional<Shipment> = jpa.findByTrackingNumber(trackingNumber)
    override fun findByMemberId(memberId: Long, pageable: Pageable): Page<Shipment> = jpa.findByMemberId(memberId, pageable)
}
