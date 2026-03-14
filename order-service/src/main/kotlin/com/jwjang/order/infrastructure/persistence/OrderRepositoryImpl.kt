package com.jwjang.order.infrastructure.persistence

import com.jwjang.order.domain.model.Order
import com.jwjang.order.domain.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

interface OrderJpaRepository : JpaRepository<Order, Long> {
    fun findByOrderNumber(orderNumber: String): Optional<Order>
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<Order>
}

@Repository
class OrderRepositoryImpl(
    private val jpa: OrderJpaRepository
) : OrderRepository {
    override fun save(order: Order): Order = jpa.save(order)
    override fun findById(id: Long): Optional<Order> = jpa.findById(id)
    override fun findByOrderNumber(orderNumber: String): Optional<Order> = jpa.findByOrderNumber(orderNumber)
    override fun findByMemberId(memberId: Long, pageable: Pageable): Page<Order> = jpa.findByMemberId(memberId, pageable)
}
