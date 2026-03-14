package com.jwjang.order.domain.repository

import com.jwjang.order.domain.model.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: Long): Optional<Order>
    fun findByOrderNumber(orderNumber: String): Optional<Order>
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<Order>
}
